package me.THEREALWWEFAN231.tunnelmc.translator.packet;

import org.cloudburstmc.protocol.bedrock.packet.ResourcePackClientResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;

import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;

public class ResourcePackStackPacketTranslator extends PacketTranslator<ResourcePackStackPacket> {

	@Override
	public void translate(ResourcePackStackPacket packet) {
		ResourcePackClientResponsePacket resourcePackClientResponsePacket = new ResourcePackClientResponsePacket();
		resourcePackClientResponsePacket.setStatus(ResourcePackClientResponsePacket.Status.COMPLETED);

		Client.instance.sendPacketImmediately(resourcePackClientResponsePacket);
	}

	@Override
	public Class<?> getPacketClass() {
		return ResourcePackStackPacket.class;
	}

}
