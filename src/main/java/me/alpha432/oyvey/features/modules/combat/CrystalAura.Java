package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.Direction;

import java.util.*;
import java.util.stream.Collectors;

public class CrystalAura extends Module {
    private long lastActionTime = 0;

    public CrystalAura() {
        super("CrystalAura", "Hits and places crystals automatically", Category.COMBAT, true, false, false);
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        if (now - lastActionTime < 50) return; // 20 ticks per second = 50ms delay

        EndCrystalEntity target = mc.world.getEntities()
            .stream()
            .filter(e -> e instanceof EndCrystalEntity)
            .map(e -> (EndCrystalEntity) e)
            .filter(this::canSee)
            .sorted(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
            .findFirst().orElse(null);

        if (target != null && mc.player.getAttackCooldownProgress(0.0f) >= 1.0f) {
            // Rotate and attack
            lookAt(target.getPos());
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.interactionManager.attackEntity(mc.player, target);
            lastActionTime = now;
            return;
        }

        // Try to place if holding crystals
        int crystalSlot = findCrystalInHotbar();
        if (crystalSlot == -1) return;

        BlockPos pos = findPlaceableBlock();
        if (pos == null) return;

        Vec3d vec = Vec3d.ofCenter(pos.up());

        lookAt(vec);

        mc.player.getInventory().selectedSlot = crystalSlot;
        BlockHitResult bhr = new BlockHitResult(vec, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.player.swingHand(Hand.MAIN_HAND);

        lastActionTime = now;
    }

    private boolean canSee(EndCrystalEntity crystal) {
        return mc.player.canSee(crystal) && mc.player.distanceTo(crystal) <= 4.5f;
    }

    private int findCrystalInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) {
                return i;
            }
        }
        return -1;
    }

    private BlockPos findPlaceableBlock() {
        BlockPos playerPos = mc.player.getBlockPos();

        for (BlockPos pos : BlockPos.iterateOutwards(playerPos, 4, 4, 4)) {
            if (isValidPlacePos(pos)) {
                return pos;
            }
        }
        return null;
    }

    private boolean isValidPlacePos(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        Block above = mc.world.getBlockState(pos.up()).getBlock();
        Block above2 = mc.world.getBlockState(pos.up(2)).getBlock();

        boolean isBase = block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
        boolean isAirAbove = above == Blocks.AIR && above2 == Blocks.AIR;

        return isBase && isAirAbove && mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= 16;
    }

    private void lookAt(Vec3d pos) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d diff = pos.subtract(eyes);
        double diffXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, diffXZ));

        mc.player.setYaw(smoothYaw(mc.player.getYaw(), yaw, 10f));
        mc.player.setPitch(smoothPitch(mc.player.getPitch(), pitch, 10f));
    }

    private float smoothYaw(float current, float target, float speed) {
        float delta = wrapDegrees(target - current);
        delta = Math.max(-speed, Math.min(speed, delta));
        return current + delta;
    }

    private float smoothPitch(float current, float target, float speed) {
        float delta = target - current;
        delta = Math.max(-speed, Math.min(speed, delta));
        return current + delta;
    }

    private float wrapDegrees(float degrees) {
        degrees %= 360f;
        if (degrees >= 180f) degrees -= 360f;
        if (degrees < -180f) degrees += 360f;
        return degrees;
    }
}
