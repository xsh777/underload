package anonymous.underload;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;

public class BulletproofBuffer extends FriendlyByteBuf {

    private static final long NBT_SIZE_LIMIT = 10000L;
    private static final long NBT_READ_TIME_LIMIT = 1000L; //in ms

    public BulletproofBuffer(ByteBuf parent) {
        super(parent);
    }

    @Override @Nullable
    public CompoundTag readNbt() {
        long start = System.currentTimeMillis();

        CompoundTag tag = this.readNbt(new NbtAccounter(NBT_SIZE_LIMIT));

        long end = System.currentTimeMillis();

        if (end - start >= NBT_READ_TIME_LIMIT) {
            throw new RuntimeException("It took too long to read NBT tag");
        }

        return tag;
    }
}
