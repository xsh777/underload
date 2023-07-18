package anonymous.underload;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class PacketsNameLogger extends ChannelDuplexHandler {

    private final Logger logger;

    public PacketsNameLogger() {
        this.logger = LoggerFactory.getLogger("PacketsNameLogger");
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
        this.logger.info(msg.getClass().getSimpleName());
        ctx.fireChannelRead(msg);
    }
}
