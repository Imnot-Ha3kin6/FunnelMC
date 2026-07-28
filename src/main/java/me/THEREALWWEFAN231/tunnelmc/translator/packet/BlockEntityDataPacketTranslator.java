package me.THEREALWWEFAN231.tunnelmc.translator.packet;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;

import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;

public class BlockEntityDataPacketTranslator extends PacketTranslator<BlockEntityDataPacket> {

	@Override
	public void translate(BlockEntityDataPacket packet) {
		
		Vector3i blockPosition = packet.getBlockPosition();
		NbtMap blockEntityData = packet.getData();
		
		Client.instance.blockEntityDataCache.getCachedBlockPositionsData().put(blockPosition, blockEntityData);
	}

	@Override
	public Class<?> getPacketClass() {
		return BlockEntityDataPacket.class;
	}

}
