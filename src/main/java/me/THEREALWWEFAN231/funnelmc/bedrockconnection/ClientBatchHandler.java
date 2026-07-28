package me.THEREALWWEFAN231.funnelmc.bedrockconnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.common.PacketSignal;

import me.THEREALWWEFAN231.funnelmc.FunnelMC;
import net.minecraft.client.Minecraft;

public class ClientBatchHandler implements BedrockPacketHandler {

	private Logger logger = LogManager.getLogger(ClientBatchHandler.class);

	@Override
	public PacketSignal handlePacket(BedrockPacket packet) {
		if (Client.instance.bedrockSession != null && Client.instance.bedrockSession.isLogging()) {
			//so yeah.... the default logger, in nukkitx is kind of lame, and in our case trace isn't enabled so we will just do this for now
			this.logger.info("Inbound {}: {}", Client.instance.bedrockSession.getSocketAddress(), packet.toString().substring(0, Math.min(packet.toString().length(), 200)));
		}

		// The server's reply to RequestNetworkSettingsPacket - has to be handled here rather than
		// through the regular translator pipeline since it's part of pre-login handshake, not
		// gameplay, and it's what unblocks actually sending LoginPacket.
		if (packet instanceof NetworkSettingsPacket) {
			Client.instance.onNetworkSettings((NetworkSettingsPacket) packet);
			return PacketSignal.HANDLED;
		}

		// LevelChunkPacket#getData() is a Netty ByteBuf sliced out of the inbound frame, sharing that
		// frame's refCnt rather than owning independent memory - normally fine since the pipeline
		// releases the frame right after this handler returns, but we defer actual reading of it to
		// LevelChunkTranslator on the main thread below, by which point the pipeline's release has
		// already dropped it to refCnt 0. Retain it here on the Netty thread instead, so it survives
		// until LevelChunkTranslator releases it once it's done copying out of it.
		if (packet instanceof LevelChunkPacket) {
			((LevelChunkPacket) packet).getData().retain();
		}

		// Translators touch client-only state (mc.level, mc.player, screens, ...) that vanilla
		// guards with RunningOnDifferentThreadException outside the render/main thread. This
		// handler runs on the RakNet session's Netty I/O thread, not the main thread, so translation
		// has to be handed off instead of run inline here.
		Minecraft.getInstance().execute(() -> FunnelMC.instance.packetTranslatorManager.translatePacket(packet));

		return PacketSignal.HANDLED;
	}

}
