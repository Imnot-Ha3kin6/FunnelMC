package me.THEREALWWEFAN231.tunnelmc.javaconnection.packet.movement;

import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;

import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class PositionOnlyTranslator extends PacketTranslator<ServerboundMovePlayerPacket.Pos> {

	@Override
	public void translate(ServerboundMovePlayerPacket.Pos packet) {
		PlayerMoveTranslator.translateMovementPacket(packet, MovePlayerPacket.Mode.NORMAL);
	}

	@Override
	public Class<?> getPacketClass() {
		return ServerboundMovePlayerPacket.Pos.class;
	}

}
