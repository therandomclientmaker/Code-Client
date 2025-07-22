package me.alpha432.oyvey.features.modules.combat;

import com.google.common.eventbus.Subscribe;
import me.alpha432.oyvey.event.impl.PacketEvent;
import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.util.Hand;

public class Criticals extends Module {
    private int tickDelay = 0;
    private Entity queuedTarget = null;

    public Criticals() {
        super("Criticals", "Makes you do critical hits", Category.COMBAT, true, false, false);
    }

    @Subscribe
    private void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet &&
                packet.type.getType() == InteractType.ATTACK) {

            Entity entity = mc.world.getEntityById(packet.entityId);
            if (entity == null || entity instanceof EndCrystalEntity || !(entity instanceof LivingEntity)) return;

            if (mc.player.isOnGround()) {
                // Start jump + queue the attack
                mc.player.jump();
                tickDelay = 2; // wait 2 ticks for downward motion
                queuedTarget = entity;
                event.setCancelled(true); // cancel the attack for now
            }
        }
    }

    @Override
    public void onTick() {
        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        if (queuedTarget != null && mc.player.fallDistance > 0.1 && !mc.player.isOnGround()) {
            // Perform the attack mid-air
            mc.interactionManager.attackEntity(mc.player, queuedTarget);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.addCritParticles(queuedTarget);
            queuedTarget = null;
        }
    }

    @Override
    public String getDisplayInfo() {
        return "LegitJump";
    }
}
