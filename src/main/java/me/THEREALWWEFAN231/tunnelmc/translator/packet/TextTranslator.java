package me.THEREALWWEFAN231.tunnelmc.translator.packet;

import org.cloudburstmc.protocol.bedrock.packet.TextPacket;

import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

public class TextTranslator extends PacketTranslator<TextPacket> {

	@Override
	public void translate(TextPacket packet) {
		switch (packet.getType()) {
			default: {
				System.out.println("Falling back to raw translation for " + packet.toString());
			}
			case RAW: {
				ClientboundSystemChatPacket clientboundSystemChatPacket = new ClientboundSystemChatPacket(Component.literal(packet.getMessage()), false);

				Client.instance.javaConnection.processServerToClientPacket(clientboundSystemChatPacket);
				break;
			}
			case CHAT: {
				String formattedChatMessage = "<" + packet.getSourceName() + "> " + packet.getMessage();
				ClientboundSystemChatPacket clientboundSystemChatPacket = new ClientboundSystemChatPacket(Component.literal(formattedChatMessage), false);

				Client.instance.javaConnection.processServerToClientPacket(clientboundSystemChatPacket);
				break;
			}
		}
	}

	@Override
	public Class<?> getPacketClass() {
		return TextPacket.class;
	}
}
