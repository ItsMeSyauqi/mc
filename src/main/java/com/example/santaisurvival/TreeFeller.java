package com.example.santaisurvival;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class TreeFeller {

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            SantaiConfig cfg = SantaiSurvival.CONFIG;
            if (!(world instanceof ServerWorld sw) || !(player instanceof ServerPlayerEntity sp)) return;
            if (cfg.treeFellerRequireSneak && !sp.isSneaking()) return;
            if (!state.isIn(BlockTags.LOGS)) return;

            ItemStack tool = sp.getMainHandStack();
            if (tool.isEmpty() || !tool.isIn(ItemTags.AXES)) return;

            if (sw.getBlockState(pos.down()).isIn(BlockTags.LOGS)) return;

            fell(sw, sp, pos.toImmutable(), tool, cfg);
        });
    }

    private static void fell(ServerWorld world, ServerPlayerEntity player, BlockPos origin,
                             ItemStack tool, SantaiConfig cfg) {
        List<BlockPos> logs = collectLogs(world, origin, cfg.treeFellerMaxLogs, cfg.treeFellerOnlyNaturalTrees);
        if (logs == null || logs.isEmpty()) return;

        logs.sort(Comparator.comparingInt(BlockPos::getY));

        int felled = 0;
        for (BlockPos p : logs) {
            BlockState s = world.getBlockState(p);
            if (!s.isIn(BlockTags.LOGS)) continue;

            if (player.isCreative()) {
                world.removeBlock(p, false);
                felled++;
                continue;
            }
            Block.dropStacks(s, world, p, null, player, tool);
            world.breakBlock(p, false);
            felled++;

            tool.damage(1, player, EquipmentSlot.MAINHAND);
            if (tool.isEmpty()) break;
        }
        if (felled > 0) {
            player.sendMessage(Text.literal("Tebang pohon: +" + felled + " kayu"), true);
        }
    }

    private static List<BlockPos> collectLogs(ServerWorld w, BlockPos origin, int max, boolean needNatural) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> result = new ArrayList<>();
        boolean naturalLeaves = false;

        seen.add(origin);
        queue.add(origin);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (!naturalLeaves) naturalLeaves = hasNaturalLeaves(w, cur);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (result.size() >= max) continue;
                        BlockPos n = cur.add(dx, dy, dz);
                        if (!seen.add(n)) continue;
                        if (w.getBlockState(n).isIn(BlockTags.LOGS)) {
                            result.add(n);
                            queue.add(n);
                        }
                    }
                }
            }
        }
        if (needNatural && !naturalLeaves) return null;
        return result;
    }

    private static boolean hasNaturalLeaves(ServerWorld w, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState s = w.getBlockState(pos.offset(dir));
            if (s.isIn(BlockTags.LEAVES) && s.contains(LeavesBlock.PERSISTENT)
                    && !s.get(LeavesBlock.PERSISTENT)) {
                return true;
            }
        }
        return false;
    }
}
