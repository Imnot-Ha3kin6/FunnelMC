package me.THEREALWWEFAN231.funnelmc.javaconnection;

import java.util.Collections;

import com.mojang.authlib.GameProfile;

import me.THEREALWWEFAN231.funnelmc.FunnelMC;
import me.THEREALWWEFAN231.funnelmc.bedrockconnection.Client;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.ServerLinks;
import net.minecraft.world.flag.FeatureFlags;

public class FakeJavaConnection {

	private final Connection connection;//TODO: i think we dont need this to be in the "public scope"
	private final ClientPacketListener clientPacketListener;
	public JavaPacketTranslatorManager packetTranslatorManager;

	public FakeJavaConnection() {
		this.connection = new Connection(PacketFlow.CLIENTBOUND);
		GameProfile gameProfile = new GameProfile(Client.instance.authData.getIdentity(), Client.instance.authData.getDisplayName());
		// TODO: several of these cookie fields are placeholders (null/empty) since we have no
		// real Java server to source them from. Registry access is rebuilt from vanilla's bundled
		// default data (see VanillaRegistryAccess) instead of a real server's registry-sync
		// packets - ClientPacketListener's constructor and ClientLevel both require dynamic
		// registries (worldgen/biome, worldgen/dimension_type, ...) to be present, not just the
		// static item/block layer.
		RegistryAccess.Frozen registryAccess = VanillaRegistryAccess.get();
		CommonListenerCookie cookie = new CommonListenerCookie(null, gameProfile, null, registryAccess, FeatureFlags.VANILLA_SET, "funnelmc",
				null, null, Collections.emptyMap(), null, Collections.emptyMap(), ServerLinks.EMPTY, Collections.emptyMap(), false);
		this.clientPacketListener = new ClientPacketListener(FunnelMC.mc, this.connection, cookie);
		this.packetTranslatorManager = new JavaPacketTranslatorManager();
	}

	public void processServerToClientPacket(Packet<ClientGamePacketListener> packet) {
		//this is what minecraft does, Connection.channelRead0()V
		packet.handle(this.clientPacketListener);
	}

}
