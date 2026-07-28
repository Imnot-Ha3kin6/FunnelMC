package me.THEREALWWEFAN231.tunnelmc.translator.packet.entity;

import java.util.Optional;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket;
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket.State;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.dimension.DimensionTranslator;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class RespawnPacketTranslator extends PacketTranslator<RespawnPacket> {

	@Override
	public void translate(RespawnPacket packet) {

		if (packet.getState() == State.SERVER_SEARCHING) {//display death screen?

		}

		if (packet.getState() == State.SERVER_READY) {

			PlayerActionPacket playerActionPacket = new PlayerActionPacket();
			playerActionPacket.setRuntimeEntityId(TunnelMC.mc.player.getEntityId());
			playerActionPacket.setAction(PlayerActionType.RESPAWN);
			playerActionPacket.setBlockPosition(Vector3i.ZERO);
			playerActionPacket.setFace(-1);

			Client.instance.sendPacket(playerActionPacket);

			//TODO: correct these values, so like it's not just overworld, and survival
			// TODO: dimensionType is a placeholder - see DimensionTranslator/StartGameTranslator TODOs
			Holder<DimensionType> dimensionType = null;
			ResourceKey<Level> dimensionId = Level.OVERWORLD;
			CommonPlayerSpawnInfo commonPlayerSpawnInfo = new CommonPlayerSpawnInfo(dimensionType, dimensionId, -1, GameType.SURVIVAL, GameType.SURVIVAL, false, false, Optional.empty(), 0, 63);
			ClientboundRespawnPacket clientboundRespawnPacket = new ClientboundRespawnPacket(commonPlayerSpawnInfo, ClientboundRespawnPacket.KEEP_ALL_DATA);
			Client.instance.javaConnection.processServerToClientPacket(clientboundRespawnPacket);
		}

	}

	@Override
	public Class<?> getPacketClass() {
		return RespawnPacket.class;
	}

}
