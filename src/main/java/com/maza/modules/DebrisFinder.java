package com.maza.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.ChunkDataAndUpdateLightS2CPacket;
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

    private final Setting<Boolean> paletteBypass = sgGeneral.add(new BoolSetting.Builder()
        .name("palette-bypass").description("Read raw palette IDs to bypass spoof")
        .defaultValue(true).build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode").defaultValue(ShapeMode.Both).build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color").defaultValue(new SettingColor(255, 165, 0, 50)).build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color").defaultValue(new SettingColor(255, 165, 0, 255)).build());

    private Map<ChunkPos, List<BlockPos>> suspects = new ConcurrentHashMap<>();
    private int windowCount = 0;

    public DebrisFinder() {
        super(MazaAddon.CATEGORY, "debris-finder", "Finds ancient debris via packet sniffing with palette bypass");
    }

    @Override
    public void onActivate() {
        suspects.clear();
        windowCount = 0;
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof ChunkDataAndUpdateLightS2CPacket packet)) return;

        ChunkPos pos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());
        List<BlockPos> found = new ArrayList<>();

        if (paletteBypass.get()) {
            parsePalette(packet, pos, found);
        } else {
            parseWorldState(pos, found);
        }

        if (!found.isEmpty()) {
            suspects.put(pos, found);
            windowCount++;
            info("Chunk " + pos + " | Suspects: " + found.size() + " | Window: " + windowCount);
        }
    }

    private void parsePalette(ChunkDataAndUpdateLightS2CPacket packet, ChunkPos pos, List<BlockPos> out) {
        try {
            var chunkData = packet.getChunkData();
            for (int y = -4; y < 20; y++) {
                var section = chunkData.getSection(y);
                if (section == null || section.isEmpty()) continue;

                var container = section.getBlockStateContainer();
                var palette = container.getPalette();
                int debrisId = -1;

                for (int i = 0; i < palette.getSize(); i++) {
                    if (palette.get(i).getBlock() == Blocks.ANCIENT_DEBRIS) {
                        debrisId = i;
                        break;
                    }
                }
                if (debrisId == -1) continue;

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int localY = 0; localY < 16; localY++) {
                            if (container.get(x, localY, z) == debrisId) {
                                int worldY = y * 16 + localY;
                                out.add(new BlockPos(pos.x * 16 + x, worldY, pos.z * 16 + z));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            parseWorldState(pos, out);
        }
    }

    private void parseWorldState(ChunkPos pos, List<BlockPos> out) {
        if (mc.world == null) return;
        WorldChunk chunk = mc.world.getChunk(pos.x, pos.z);
        if (chunk == null) return;
        for (int y = -64; y < 320; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos p = new BlockPos(pos.x * 16 + x, y, pos.z * 16 + z);
                    if (chunk.getBlockState(p).getBlock() == Blocks.ANCIENT_DEBRIS) out.add(p);
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
