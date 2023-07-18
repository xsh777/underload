package anonymous.underload;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttackDebugger implements Listener {

    private static final int ONE_TPS_TO_MS = 50;

    private final Logger logger;

    private final LastPacketsStorage lastPacketsStorage;

    public AttackDebugger(LastPacketsStorage lastPacketsStorage) {
        this.logger = LoggerFactory.getLogger("AttackDebugger");
        this.lastPacketsStorage = lastPacketsStorage;
    }

    @EventHandler
    public void dumpLastPacketsOnTooLongTick(ServerTickEndEvent event) {
        double tickDuration = event.getTickDuration();

        StringBuilder log = new StringBuilder();

        if (tickDuration >= ONE_TPS_TO_MS) {
            for (Object packet : this.lastPacketsStorage.getLastPackets()) {
                Class<?> clazz = packet.getClass();
                String name = clazz.getSimpleName();
                log.append(name).append("\t");
            }
        }

        this.logger.info(log.toString());
    }
}
