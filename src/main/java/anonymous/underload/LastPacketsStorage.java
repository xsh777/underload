package anonymous.underload;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@ChannelHandler.Sharable
public class LastPacketsStorage extends ChannelDuplexHandler {

    private static final int LAST_PACKETS_STORAGE_SIZE = 100;

    private final Queue<Object> lastPackets;

    public LastPacketsStorage() {
        this.lastPackets = Queues.synchronizedQueue(EvictingQueue.create(LAST_PACKETS_STORAGE_SIZE));
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
        this.lastPackets.add(msg);
        ctx.fireChannelRead(msg);
    }

    public Collection<Object> getLastPackets() {
        synchronized (this.lastPackets) {
            return new ArrayList<>(this.lastPackets);
        }
    }
}
