package anonymous.underload;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bstats.bukkit.Metrics;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Underload extends JavaPlugin implements Listener {

    private static final int BSTATS_PLUGIN_ID = 19095;

    private PacketsNameLogger packetsNameLogger;
    private LastPacketsStorage lastPacketsStorage;

    private DatabaseReader asnReader;
    private DatabaseReader countryReader;

    private Metrics metrics;

    private static final long SAME_NAME_LENGTH_COOOLDOWN = TimeUnit.SECONDS.toMillis(60);
    private static final int MAX_SAME_NAME_LENGTH_SERIES = 10;
    private static final String SAME_NAME_LENGTH_MESSAGE = "Nie mozesz teraz wejsc na serwer. Sprobuj ponownie za chwile.";

    private int sameNameLengthCount = 0;
    private int lastNameLength = 0;
    private long cooldown = 0L;

    private final Object lock = new Object();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        this.metrics = new Metrics(this, BSTATS_PLUGIN_ID);

        try {
            this.asnReader = this.loadMaxmindDb("ASN");
            this.countryReader = this.loadMaxmindDb("Country");
        } catch (IOException exc) {
            this.getSLF4JLogger().error("couldn't load geolite databases");
            this.setEnabled(false);
            return;
        }

        this.packetsNameLogger = new PacketsNameLogger();
        this.lastPacketsStorage = new LastPacketsStorage();

        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new AttackDebugger(this.lastPacketsStorage), this);
    }

    private DatabaseReader loadMaxmindDb(String type) throws IOException {
        return new DatabaseReader.Builder(this.getClass().getResourceAsStream("/GeoLite2-" + type + ".mmdb"))
                .withCache(new CHMCache())
                .build();
    }

    @Override
    public void onDisable() {
        this.metrics.shutdown();

        for (Player player : this.getServer().getOnlinePlayers()) {
            ChannelPipeline pipeline = this.resolvePipeline(player);

            pipeline.remove("PacketsNameLogger");
            pipeline.remove("BadPacketsFilter");
            pipeline.remove("LastPacketsStorage");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void blockProxiesAndVpns(AsyncPlayerPreLoginEvent event) {

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void blockFloods(AsyncPlayerPreLoginEvent event) {
        synchronized (this.lock) {
            String nick = event.getName();
            int nickLength = nick.length();

            if (nickLength == this.lastNameLength) {
                this.sameNameLengthCount += 1;

                if (this.sameNameLengthCount >= MAX_SAME_NAME_LENGTH_SERIES) {
                    this.cooldown = System.currentTimeMillis() + SAME_NAME_LENGTH_COOOLDOWN;
                    this.sameNameLengthCount = 0;
                }

                if (this.cooldown > System.currentTimeMillis()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(SAME_NAME_LENGTH_MESSAGE));
                }

                return;
            }

            this.sameNameLengthCount = 0;
            this.lastNameLength = nickLength;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void blockBadNetworks(AsyncPlayerPreLoginEvent event) {
        InetAddress address = event.getAddress();

        String countryIsoCode;
        String asnOrganizationNumber;

        try {
            countryIsoCode = this.countryReader.country(address).getCountry().getIsoCode();
            asnOrganizationNumber = String.valueOf(this.asnReader.asn(address).getAutonomousSystemNumber());
        } catch (IOException | GeoIp2Exception exc) {
            return;
        }

        List<String> blacklist = this.getConfig().getStringList("blacklist");

        if (blacklist.contains(countryIsoCode) || blacklist.contains(asnOrganizationNumber)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("bad network"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void injectCustomPacketHandlers(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        ChannelPipeline pipeline = this.resolvePipeline(player);

        pipeline.addBefore("packet_handler", "PacketsNameLogger", this.packetsNameLogger);
        pipeline.addBefore("decoder", "BadPacketsFilter", new BadPacketsFilter());
        pipeline.addAfter("decoder", "LastPacketsStorage", this.lastPacketsStorage);
    }

    private ChannelPipeline resolvePipeline(Player player) {
        CraftPlayer bukkitPlayer = (CraftPlayer) player;
        ServerPlayer nmsPlayer = bukkitPlayer.getHandle();

        ServerGamePacketListenerImpl gamePacketHandler = nmsPlayer.connection;
        Connection connection = gamePacketHandler.connection;
        Channel channel = connection.channel;

        return channel.pipeline();
    }
}
