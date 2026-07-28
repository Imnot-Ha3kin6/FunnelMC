package me.THEREALWWEFAN231.tunnelmc.bedrockconnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.common.PacketSignal;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;

public class ClientBatchHandler implements BedrockPacketHandler {

	private Logger logger = LogManager.getLogger(ClientBatchHandler.class);

	@Override
	public PacketSignal handlePacket(BedrockPacket packet) {
		if (Client.instance.bedrockSession != null && Client.instance.bedrockSession.isLogging()) {
			//so yeah.... the default logger, in nukkitx is kind of lame, and in our case trace isn't enabled so we will just do this for now
			this.logger.info("Inbound {}: {}", Client.instance.bedrockSession.getSocketAddress(), packet.toString().substring(0, Math.min(packet.toString().length(), 200)));
		}

		TunnelMC.instance.packetTranslatorManager.translatePacket(packet);

		return PacketSignal.HANDLED;
	}

}
