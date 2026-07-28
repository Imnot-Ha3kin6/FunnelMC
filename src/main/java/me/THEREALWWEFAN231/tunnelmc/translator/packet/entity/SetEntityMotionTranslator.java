package me.THEREALWWEFAN231.tunnelmc.translator.packet.entity;

import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public class SetEntityMotionTranslator extends PacketTranslator<SetEntityMotionPacket>{

	@Override
	public void translate(SetEntityMotionPacket packet) {
		if (TunnelMC.mc.level == null) {
			// Stack trace thrown otherwise
			return;
		}
		int id = (int) packet.getRuntimeEntityId();
		Vec3 velocity = new Vec3(packet.getMotion().getX(), packet.getMotion().getY(), packet.getMotion().getZ());

		ClientboundSetEntityMotionPacket entityVelocityUpdateS2CPacket = new ClientboundSetEntityMotionPacket(id, velocity);
		Client.instance.javaConnection.processServerToClientPacket(entityVelocityUpdateS2CPacket);
	}

	@Override
	public Class<?> getPacketClass() {
		return SetEntityMotionPacket.class;
	}

}
