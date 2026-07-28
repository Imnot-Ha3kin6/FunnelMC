package me.THEREALWWEFAN231.tunnelmc.translator.packet.entity;

import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;

public class AnimateTranslator extends PacketTranslator<AnimatePacket> {
    @Override
    public void translate(AnimatePacket packet) {
        if (TunnelMC.mc.level == null) {
            return;
        }
        Entity entity = TunnelMC.mc.level.getEntity((int) packet.getRuntimeEntityId());
        if (entity == null) {
            return;
        }

        switch (packet.getAction()) {
            case SWING_ARM:
                ClientboundAnimatePacket swingArmPacket = new ClientboundAnimatePacket(entity, 0);
                Client.instance.javaConnection.processServerToClientPacket(swingArmPacket);
                break;
        }
    }

    @Override
    public Class<?> getPacketClass() {
        return AnimatePacket.class;
    }
}
