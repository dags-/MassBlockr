package me.dags.massblockr.minecraft.world.chunk;

import me.dags.massblockr.jnbt.*;
import me.dags.massblockr.minecraft.block.BlockState;
import me.dags.massblockr.minecraft.palette.Palette;
import me.dags.massblockr.minecraft.world.LegacyBlockHandler;
import me.dags.massblockr.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author dags <dags@dags.me>
 */
public class LegacyChunk implements Chunk, LegacyBlockHandler {

    private static final byte[] NONE = new byte[0];
    private static final ByteArrayTag EMPTY = new ByteArrayTag("EMPTY", NONE);

    private final Palette palette;
    private final CompoundTag chunk;
    private final ByteArrayTag[] blocks;
    private final ByteArrayTag[] adds;
    private final ByteArrayTag[] data;
    private final ByteArrayTag biomes;
    private final int sectionCount;
    private final int maxHeight;
    private final int x, z;

    public LegacyChunk(World world, int x, int z, CompoundTag chunk) {
        CompoundTag level = (CompoundTag) chunk.getTag("Level");
        ListTag sections = (ListTag) level.getTag("Sections");

        this.x = x;
        this.z = z;
        this.chunk = chunk;
        this.sectionCount = sections.getValue().size();
        this.blocks = new ByteArrayTag[sectionCount];
        this.adds = new ByteArrayTag[sectionCount];
        this.data = new ByteArrayTag[sectionCount];
        this.biomes = (ByteArrayTag) chunk.getTag("Biomes");
        this.maxHeight = sectionCount << 5; // (max + 1) << 5 ?
        this.palette = world.getRegistry().getGlobalPalette();

        for (int i = 0; i < sectionCount; i++) {
            CompoundTag section = (CompoundTag) sections.getValue().get(i);
            blocks[i] = (ByteArrayTag) section.getTag("Blocks");
            adds[i] = section.containsTag("Adds") ? (ByteArrayTag) section.getTag("Adds") : EMPTY;
            data[i] = (ByteArrayTag) section.getTag("Data");
        }
    }

    @Override
    public int getHeight() {
        return maxHeight;
    }

    @Override
    public int getSectionCount() {
        return sectionCount;
    }

    @Override
    public CompoundTag getTag() {
        return chunk;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public Palette getPalette(int sectionIndex) {
        return palette;
    }

    @Override
    public int getBiome(int biomeIndex) {
        return biomes.getValue()[biomeIndex];
    }

    @Override
    public void setBiome(int biomeIndex, int biome) {
        biomes.getValue()[biomeIndex] = (byte) biome;
    }

    @Override
    public BlockState getState(int sectionIndex, int blockIndex) {
        byte[] blocks = this.blocks[sectionIndex].getValue();
        byte[] adds = this.adds[sectionIndex].getValue();
        byte[] data = this.data[sectionIndex].getValue();
        int id = getShort(blockIndex, blocks, adds);
        int meta = getNibble(blockIndex, data);
        int stateId = getStateId(id, meta);
        return getPalette(sectionIndex).getState(stateId);
    }

    @Override
    public void setState(int sectionIndex, int blockIndex, BlockState state) {
        int stateId = getPalette(sectionIndex).getStateId(state);
        int blockId = getBlockId(stateId);
        int metaData = getMetaData(stateId);

        byte[] blocks = this.blocks[sectionIndex].getValue();
        byte[] adds = this.adds[sectionIndex].getValue();
        byte[] data = this.data[sectionIndex].getValue();

        byte[] newAdds = setShort(blockIndex, blockId, blocks, adds);
        setNibble(blockIndex, metaData, data);

        if (newAdds != adds) {
            this.adds[sectionIndex] = new ByteArrayTag("Adds", newAdds);
        }
    }

    public static Chunk createNewChunk(World world, Chunk input) {
        if (input instanceof LegacyChunk) {
            return input;
        }

        List<Tag> list = new ArrayList<>(input.getSectionCount());
        for (int i = 0; i < input.getSectionCount(); i++) {
            list.add(i, new ByteArrayTag("" + i, new byte[256 * 16]));
        }

        CompoundTag level = new CompoundTag("Level", new HashMap<>());
        level.setTag("Sections", new ListTag("Sections", NBTConstants.TYPE_BYTE_ARRAY, list));

        CompoundTag chunk = new CompoundTag("", Collections.singletonMap("Level", level)); // name should be chunk id (x,z) ??

        Chunk.shallowCopy(input.getTag(), chunk);
        return new LegacyChunk(world, input.getX(), input.getZ(), chunk);
    }
}
