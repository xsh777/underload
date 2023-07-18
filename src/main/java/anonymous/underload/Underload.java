package anonymous.underload;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bstats.bukkit.Metrics;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Underload extends JavaPlugin implements Listener {

    private static final int BSTATS_PLUGIN_ID = 19095;

    private PacketsNameLogger packetsNameLogger;
    private LastPacketsStorage lastPacketsStorage;

    @Override
    public void onEnable() {
        new Metrics(this, BSTATS_PLUGIN_ID);

        this.packetsNameLogger = new PacketsNameLogger();
        this.lastPacketsStorage = new LastPacketsStorage();

        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new AttackDebugger(this.lastPacketsStorage), this);
    }

    @Override
    public void onDisable() {
        for (Player player : this.getServer().getOnlinePlayers()) {
            ChannelPipeline pipeline = this.resolvePipeline(player);

            pipeline.remove("PacketsNameLogger");
            pipeline.remove("BadPacketsFilter");
            pipeline.remove("LastPacketsStorage");
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
