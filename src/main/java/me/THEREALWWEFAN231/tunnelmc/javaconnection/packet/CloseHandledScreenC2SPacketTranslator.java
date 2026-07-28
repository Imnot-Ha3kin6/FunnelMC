package me.THEREALWWEFAN231.tunnelmc.javaconnection.packet;

import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;

import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.mixins.interfaces.IMixinCloseHandledScreenC2SPacket;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

public class CloseHandledScreenC2SPacketTranslator extends PacketTranslator<CloseHandledScreenC2SPacket> {

	@Override
	public void translate(CloseHandledScreenC2SPacket packet) {

		ContainerClosePacket containerClosePacket = new ContainerClosePacket();
		containerClosePacket.setId((byte) ((IMixinCloseHandledScreenC2SPacket) packet).getSyncId());
		
		Client.instance.sendPacket(containerClosePacket);
	}

	@Override
	public Class<?> getPacketClass() {
		return CloseHandledScreenC2SPacket.class;
	}

}