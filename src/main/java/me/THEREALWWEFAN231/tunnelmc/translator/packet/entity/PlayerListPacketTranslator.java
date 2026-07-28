package me.THEREALWWEFAN231.tunnelmc.translator.packet.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;

import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.mixins.interfaces.IMixinPlayerListS2CPacket;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;

public class PlayerListPacketTranslator extends PacketTranslator<PlayerListPacket> {

	@Override
	public void translate(PlayerListPacket packet) {
		boolean add = packet.getAction() == PlayerListPacket.Action.ADD;

		if (!add) {
			List<UUID> removedProfileIds = new ArrayList<>();
			for (PlayerListPacket.Entry entry : packet.getEntries()) {
				removedProfileIds.add(entry.getUuid());
			}

			ClientboundPlayerInfoRemovePacket removePacket = new ClientboundPlayerInfoRemovePacket(removedProfileIds);
			Client.instance.javaConnection.processServerToClientPacket(removePacket);
			return;
		}

		List<ClientboundPlayerInfoUpdatePacket.Entry> entries = new ArrayList<>();
		for (PlayerListPacket.Entry entry : packet.getEntries()) {
			// gamemode says nullable but is used in ClientGameSession/:
			entries.add(new ClientboundPlayerInfoUpdatePacket.Entry(entry.getUuid(), new GameProfile(entry.getUuid(), entry.getName()),
					true, 0, GameType.SURVIVAL, Component.literal(entry.getName()), true, 0, null));
		}

		ClientboundPlayerInfoUpdatePacket playerListS2CPacket = new ClientboundPlayerInfoUpdatePacket(
				EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER), Collections.emptyList());
		((IMixinPlayerListS2CPacket) playerListS2CPacket).setEntries(entries);

		Client.instance.javaConnection.processServerToClientPacket(playerListS2CPacket);
	}

	@Override
	public Class<?> getPacketClass() {
		return PlayerListPacket.class;
	}

	public static class SkinType {//this is only temporary

		public Identifier texture;
		public boolean slim;

		public SkinType(Identifier texture, boolean slim) {
			this.texture = texture;
			this.slim = slim;
		}

	}

}
