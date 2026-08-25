package com.maza.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import com.maza.MazaAddon;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DebrisFinder extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius").description("Chunk scan radius")
        .defaultValue(8).min(1).max(16).build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode").defaultValue(ShapeMode.Both).build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color").defaultValue(new SettingColor(255, 165, 0, 50)).build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color").defaultValue(new SettingColor(255, 165, 0, 255)).build());

    private Map<ChunkPos, List<BlockPos>> suspects = new ConcurrentHashMap<>();
    private int windowCount = 0;

    public DebrisFinder() {
        super(MazaAddon.CATEGORY, "debris-finder", "Finds ancient debris via chunk packet sniffing");
    }

    @Override
    public void onActivate() {
        suspects.clear();
        windowCount = 0;
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof ChunkDataS2CPacket)) return;

        ChunkPos pos = new ChunkPos(((ChunkDataS2CPacket) event.packet).getChunkX(), ((ChunkDataS2CPacket) event.packet).getChunkZ());
        List<BlockPos> found = new ArrayList<>();

        // chunk yüklendikten sonra world state'ten oku
        mc.execute(() -> scanChunk(pos, found));

        if (!found.isEmpty()) {
            suspects.put(pos, found);
            windowCount++;
            info("Chunk " + pos + " | Suspects: " + found.size() + " | Window: " + windowCount);
        }
    }

    private void scanChunk(ChunkPos pos, List<BlockPos> out) {
        if (mc.world == null) return;
        WorldChunk chunk = mc.world.getChunk(pos.x, pos.z);
        if (chunk == null) return;

        for (int y = -64; y < 320; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos p = new BlockPos(pos.x * 16 + x, y, pos.z * 16 + z);
                    if (chunk.getBlockState(p).getBlock() == Blocks.ANCIENT_DEBRIS) {
                        out.add(p);
                    }
                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (var entry : suspects.entrySet()) {
            for (BlockPos pos : entry.getValue()) {
                event.renderer.box(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                    sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }
}
