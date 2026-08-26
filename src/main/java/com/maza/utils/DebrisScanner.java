package com.maza.utils;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public class DebrisScanner {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Set<BlockPos> scanChunk(WorldChunk chunk, int minY, int maxY) {
        Set<BlockPos> found = new HashSet<>();
        if (chunk == null || chunk.isEmpty() || mc.world == null) return found;

        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;

        for (int y = minY; y <= maxY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos p = new BlockPos(cx * 16 + x, y, cz * 16 + z);
                    if (chunk.getBlockState(p).getBlock() == Blocks.ANCIENT_DEBRIS) {
                        found.add(p);
                    }
                }
            }
        }

        return found;
    }

    public static boolean isDebris(BlockPos pos) {
        if (mc.world == null) return false;
        return mc.world.getBlockState(pos).getBlock() == Blocks.ANCIENT_DEBRIS;
    }

    public static double distanceToPlayer(BlockPos pos) {
        if (mc.player == null) return Double.MAX_VALUE;
        double dx = pos.getX() + 0.5 - mc.player.getX();
        double dy = pos.getY() + 0.5 - mc.player.getY();
        double dz = pos.getZ() + 0.5 - mc.player.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
