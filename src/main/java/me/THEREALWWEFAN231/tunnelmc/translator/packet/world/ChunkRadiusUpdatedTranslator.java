package me.THEREALWWEFAN231.tunnelmc.translator.packet.world;

import org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;

import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;

public class ChunkRadiusUpdatedTranslator extends PacketTranslator<ChunkRadiusUpdatedPacket> {

	@Override
	public void translate(ChunkRadiusUpdatedPacket packet) {
		Client.instance.javaConnection.processServerToClientPacket(new ChunkLoadDistanceS2CPacket(packet.getRadius()));
	}

	@Override
	public Class<?> getPacketClass() {
		return ChunkRadiusUpdatedPacket.class;
	}

}
