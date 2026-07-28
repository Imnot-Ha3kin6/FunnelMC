package me.THEREALWWEFAN231.tunnelmc.javaconnection.packet;

import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;

import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

public class CloseHandledScreenC2SPacketTranslator extends PacketTranslator<ServerboundContainerClosePacket> {

	@Override
	public void translate(ServerboundContainerClosePacket packet) {

		ContainerClosePacket containerClosePacket = new ContainerClosePacket();
		containerClosePacket.setId((byte) packet.getContainerId());

		Client.instance.sendPacket(containerClosePacket);
	}

	@Override
	public Class<?> getPacketClass() {
		return ServerboundContainerClosePacket.class;
	}

}
