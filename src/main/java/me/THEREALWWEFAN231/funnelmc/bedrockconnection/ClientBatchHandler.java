package me.THEREALWWEFAN231.funnelmc.bedrockconnection;

import java.security.interfaces.ECPublicKey;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.ClientToServerHandshakePacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry;

import me.THEREALWWEFAN231.funnelmc.FunnelMC;
import me.THEREALWWEFAN231.funnelmc.translator.blockstate.BlockPaletteTranslator;
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

		// Enabling encryption has to happen synchronously right here on the Netty I/O thread, not
		// via the deferred Minecraft.getInstance().execute() translator path below. The server
		// starts encrypting every packet it sends immediately after this handshake packet, but the
		// Netty thread keeps decoding whatever arrives next regardless of whether the main thread
		// has gotten around to running the queued lambda yet - any packet that lands in that window
		// gets fed into the compression codec as raw ciphertext, which reads a garbage byte for the
		// compression algorithm and blows up with "Unknown compression algorithm <garbage>",
		// killing the RakNet session and forcing a reconnect. This logic touches no client-only
		// state (mc.level, mc.player, screens, ...), so it's safe to run inline here.
		if (packet instanceof ServerToClientHandshakePacket) {
			handleServerToClientHandshake((ServerToClientHandshakePacket) packet);
			return PacketSignal.HANDLED;
		}

		// Every item-bearing packet that arrives after this one (CreativeContentPacket,
		// ItemComponentPacket, CraftingDataPacket, AddItemEntityPacket, ...) gets decoded against
		// these DefinitionRegistrys - without them the codec NPEs on itemDefinitions/blockDefinitions
		// being null the moment any of those packets shows up (item stacks can carry a block-item
		// component that's decoded against blockDefinitions). This has to happen synchronously right
		// here, not inside StartGameTranslator - packet translation is deferred onto the main thread
		// below, but packet *decoding* keeps happening on this Netty thread as bytes arrive regardless
		// of whether the main thread has gotten around to running StartGamePacket's deferred translate()
		// yet, so a subsequent packet in the same batch could get decoded before that ever runs.
		if (packet instanceof StartGamePacket) {
			StartGamePacket startGamePacket = (StartGamePacket) packet;
			Client.instance.bedrockSession.getPeer().getCodecHelper().setItemDefinitions(
					SimpleDefinitionRegistry.<ItemDefinition>builder().addAll(startGamePacket.getItemDefinitions()).build());
			Client.instance.bedrockSession.getPeer().getCodecHelper().setBlockDefinitions(BlockPaletteTranslator.BLOCK_DEFINITIONS);
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

	// Thanks to proxypass, manually parse the jwt, as said in Xbox, this would be easier using the
	// jwt library but I like seeing what's actually happening
	private void handleServerToClientHandshake(ServerToClientHandshakePacket packet) {
		try {
			String[] jwtSplit = packet.getJwt().split("\\.");
			String header = new String(Base64.getDecoder().decode(jwtSplit[0]));
			JsonObject headerObject = FunnelMC.instance.fileManagement.jsonParser.parse(header).getAsJsonObject();

			String payload = new String(Base64.getDecoder().decode(jwtSplit[1]));
			JsonObject payloadObject = FunnelMC.instance.fileManagement.jsonParser.parse(payload).getAsJsonObject();

			ECPublicKey serverKey = EncryptionUtils.parseKey(headerObject.get("x5u").getAsString());
			SecretKey key = EncryptionUtils.getSecretKey(Client.instance.authData.getPrivateKey(), serverKey, Base64.getDecoder().decode(payloadObject.get("salt").getAsString()));
			Client.instance.bedrockSession.enableEncryption(key);
		} catch (Exception e) {
			this.logger.error("Failed to enable Bedrock session encryption from ServerToClientHandshakePacket", e);
		}

		ClientToServerHandshakePacket clientToServerHandshake = new ClientToServerHandshakePacket();
		Client.instance.sendPacketImmediately(clientToServerHandshake);
	}

}
