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
		if (TunnelMC.mc.world == null) {
			// Stack trace thrown otherwise
			return;
		}
		int id = (int) packet.getRuntimeEntityId();
		Vec3d velocity = new Vec3d(packet.getMotion().getX(), packet.getMotion().getY(), packet.getMotion().getZ());
		
		EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket = new EntityVelocityUpdateS2CPacket(id, velocity);
		Client.instance.javaConnection.processServerToClientPacket(entityVelocityUpdateS2CPacket);
	}

	@Override
	public Class<?> getPacketClass() {
		return SetEntityMotionPacket.class;
	}

}
