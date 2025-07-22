package me.alpha432.oyvey.features.modules.combat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Random;

public class KillAura {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final float MAX_ROTATION_SPEED = 4.0f;
    private static final float MAX_REACH = 3.8f;
    private static final double FOV_DOT_THRESHOLD = 0.98;
    private static final Random random = new Random();

    private static long lastAttackTime = 0;
    private static Entity target = null;

    // ✅ Automatically register tick event on class load
    static {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> KillAura.onTick());
    }

    public static void onTick() {
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        if (target == null || !isValidTarget(target)) {
            target = findTarget();
        }

        if (target != null) {
            rotateToTarget(client.player, target);

            if (shouldAttack(client.player, target)) {
                performAttack(target);
                lastAttackTime = System.currentTimeMillis();
            }
        }
    }

    private static boolean isValidTarget(Entity entity) {
        return entity.isAlive()
                && entity != client.player
                && client.player.distanceTo(entity) <= MAX_REACH
                && client.player.canSee(entity);
    }

    private static Entity findTarget() {
        return client.world.getEntities()
                .stream()
                .filter(KillAura::isValidTarget)
                .min(Comparator.comparingDouble(e -> client.player.distanceTo(e)))
                .orElse(null);
    }

    private static void rotateToTarget(ClientPlayerEntity player, Entity target) {
        Vec3d targetPos = target.getEyePos();
        Vec3d playerPos = player.getEyePos();

        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        float newYaw = smoothAngle(player.getYaw(), targetYaw, MAX_ROTATION_SPEED);
        float newPitch = smoothAngle(player.getPitch(), targetPitch, MAX_ROTATION_SPEED);

        player.setYaw(newYaw);
        player.setPitch(newPitch);
    }

    private static float smoothAngle(float current, float target, float maxStep) {
        float diff = MathHelper.wrapDegrees(target - current);
        return current + MathHelper.clamp(diff, -maxStep, maxStep);
    }

    private static boolean shouldAttack(ClientPlayerEntity player, Entity target) {
        if (player.getAttackCooldownProgress(0.0f) < 1.0f) return false;

        long delay = 1000L / (8 + random.nextInt(3));
        if (System.currentTimeMillis() - lastAttackTime < delay) return false;

        Vec3d lookVec = player.getRotationVec(1.0f).normalize();
        Vec3d direction = target.getEyePos().subtract(player.getEyePos()).normalize();
        return lookVec.dotProduct(direction) > FOV_DOT_THRESHOLD;
    }

    private static void performAttack(Entity target) {
        client.player.swingHand(Hand.MAIN_HAND);
        client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        client.interactionManager.attackEntity(client.player, target);
    }
}
