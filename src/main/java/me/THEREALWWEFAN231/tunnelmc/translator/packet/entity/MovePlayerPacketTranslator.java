package me.THEREALWWEFAN231.tunnelmc.translator.packet.entity;

import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

import java.util.Collections;

public class MovePlayerPacketTranslator extends PacketTranslator<MovePlayerPacket> {

	@Override
	public void translate(MovePlayerPacket packet) {

		int id = (int) packet.getRuntimeEntityId();
		double x = packet.getPosition().getX();
		double y = packet.getPosition().getY() - TunnelMC.mc.player.getEyeHeight(Pose.STANDING);
		double z = packet.getPosition().getZ();

		float yaw = packet.getRotation().getY();
		byte packedYaw = (byte) ((int) (yaw * 256.0F / 360.0F));
		float pitch = packet.getRotation().getX();
		boolean onGround = packet.isOnGround();

		if (id == TunnelMC.mc.player.getEntityId()) {
			// This works best
			PositionMoveRotation positionMoveRotation = new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, yaw, pitch);
			ClientboundPlayerPositionPacket positionPacket = new ClientboundPlayerPositionPacket(0, positionMoveRotation, Collections.emptySet());
			Client.instance.javaConnection.processServerToClientPacket(positionPacket);
			return;
		}

		PositionMoveRotation positionMoveRotation = new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, yaw, pitch);
		ClientboundTeleportEntityPacket clientboundTeleportEntityPacket = new ClientboundTeleportEntityPacket(id, positionMoveRotation, Collections.<Relative>emptySet(), onGround);

		Client.instance.javaConnection.processServerToClientPacket(clientboundTeleportEntityPacket);

		Entity entity = TunnelMC.mc.level != null ? TunnelMC.mc.level.getEntity(id) : null;
		if (entity != null) {
			ClientboundRotateHeadPacket clientboundRotateHeadPacket = new ClientboundRotateHeadPacket(entity, packedYaw);
			Client.instance.javaConnection.processServerToClientPacket(clientboundRotateHeadPacket);
		}
	}

	@Override
	public Class<?> getPacketClass() {
		return MovePlayerPacket.class;
	}

	@Override
	public boolean idleUntil() {
		return TunnelMC.mc.player != null;
	}

}
