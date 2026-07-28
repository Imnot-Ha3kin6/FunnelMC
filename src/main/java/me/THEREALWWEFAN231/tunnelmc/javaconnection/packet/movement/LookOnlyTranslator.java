package me.THEREALWWEFAN231.tunnelmc.javaconnection.packet.movement;

import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;

import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class LookOnlyTranslator extends PacketTranslator<ServerboundMovePlayerPacket.Rot> {

	@Override
	public void translate(ServerboundMovePlayerPacket.Rot packet) {
		PlayerMoveTranslator.translateMovementPacket(packet, MovePlayerPacket.Mode.HEAD_ROTATION);

	}

	@Override
	public Class<?> getPacketClass() {
		return ServerboundMovePlayerPacket.Rot.class;
	}

}
