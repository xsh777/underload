package anonymous.underload;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.IntFunction;

public class BadPacketsFilter extends ChannelDuplexHandler {

    private static final int WINDOW_CLICK_PACKET_ID = 11; //ServerboundContainerClickPacket
    private static final int CREATIVE_PACKET_ID = 43; //ServerboundSetCreativeModeSlotPacket

    private static final int MAX_CHANGED_SLOTS = 128;

    private final Logger logger;

    public BadPacketsFilter() {
        this.logger = LoggerFactory.getLogger("BadPacketsFilter");
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws RuntimeException {
        if (msg instanceof ByteBuf buffer) {
            this.tryDecode(ctx, buffer);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void tryDecode(ChannelHandlerContext ctx, ByteBuf buffer) throws RuntimeException {
        if (!ctx.channel().isActive() || !buffer.isReadable()) {
            buffer.release();
            return;
        }

        BulletproofBuffer bulletproofBuffer = new BulletproofBuffer(buffer);

        int packetId = bulletproofBuffer.readVarInt();
        this.logger.info("Packet id: {}", packetId);

        if (packetId == WINDOW_CLICK_PACKET_ID) {
            this.logger.info("Window click");

            bulletproofBuffer.readByte(); //int containerId = bulletproofBuffer.readByte();
            bulletproofBuffer.readVarInt(); //int stateId = bulletproofBuffer.readVarInt();
            bulletproofBuffer.readShort(); //int slotNum = bulletproofBuffer.readShort();
            bulletproofBuffer.readByte(); //int buttonNum = bulletproofBuffer.readByte();
            bulletproofBuffer.readEnum(ClickType.class); //ClickType clickType = bulletproofBuffer.readEnum(ClickType.class);

            IntFunction<Int2ObjectOpenHashMap<ItemStack>> intFunction = BulletproofBuffer.limitValue(Int2ObjectOpenHashMap::new, MAX_CHANGED_SLOTS);
            bulletproofBuffer.readMap(intFunction, (bufx) -> Integer.valueOf(bufx.readShort()), FriendlyByteBuf::readItem); //Int2ObjectMap<ItemStack> changedSlots = Int2ObjectMaps.unmodifiable(bulletproofBuffer.readMap(intFunction, (bufx) -> Integer.valueOf(bufx.readShort()), FriendlyByteBuf::readItem));

            bulletproofBuffer.readItem(); //ItemStack carriedItem = bulletproofBuffer.readItem();
        } else if (packetId == CREATIVE_PACKET_ID) {
            this.logger.info("Creative");

            bulletproofBuffer.readShort(); //int slotNum = bulletproofBuffer.readShort();
            bulletproofBuffer.readItem(); //ItemStack itemStack = bulletproofBuffer.readItem();
        }

        buffer.resetReaderIndex();
        ctx.fireChannelRead(buffer);
    }
}
