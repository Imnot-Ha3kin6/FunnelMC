package me.THEREALWWEFAN231.funnelmc.javaconnection;

import java.util.Collections;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;

import me.THEREALWWEFAN231.funnelmc.FunnelMC;
import me.THEREALWWEFAN231.funnelmc.bedrockconnection.Client;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
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
		// Connection.isConnected() is channel != null && channel.isOpen() - with no channel ever
		// attached, that's permanently false, which matters far beyond just networking: vanilla's own
		// MultiPlayerGameMode.tick() (called every client tick once a real login has happened) checks
		// isConnected() before calling connection.tick() - the thing that actually drives
		// ClientPacketListener.tick(), which is what advances LevelLoadTracker (the "Loading terrain"
		// screen's dismissal condition) and everything else Connection.tick() covers (telemetry,
		// deferred packets, ...). Otherwise it silently takes the disconnected-else-branch every tick
		// forever (itself a no-op too, since handleDisconnection() also requires a non-null channel) -
		// nothing crashes, but nothing that depends on periodic ticking ever runs either. An
		// EmbeddedChannel is enough to make channelActive() fire and give Connection a real, open
		// channel - we don't need actual byte transport through it, since packets are handled directly
		// via processServerToClientPacket() below rather than by decoding real bytes off this channel.
		new EmbeddedChannel(this.connection);
		GameProfile gameProfile = new GameProfile(Client.instance.authData.getIdentity(), Client.instance.authData.getDisplayName());
		// TODO: several of these cookie fields are placeholders (null/empty) since we have no
		// real Java server to source them from. Registry access is rebuilt from vanilla's bundled
		// default data (see VanillaRegistryAccess) instead of a real server's registry-sync
		// packets - ClientPacketListener's constructor and ClientLevel both require dynamic
		// registries (worldgen/biome, worldgen/dimension_type, ...) to be present, not just the
		// static item/block layer.
		RegistryAccess.Frozen registryAccess = VanillaRegistryAccess.get();
		// ClientCommonPacketListenerImpl hard-requires this to be non-null - every packet handler that
		// touches world/time state (handleLogin, handleSetTime, ...) calls straight into it with no
		// null check, since a real server connection always gets one from Minecraft#getTelemetryManager.
		WorldSessionTelemetryManager telemetryManager = FunnelMC.mc.getTelemetryManager()
				.createWorldSessionManager(true, null, null, UUID.randomUUID());
		CommonListenerCookie cookie = new CommonListenerCookie(null, gameProfile, telemetryManager, registryAccess, FeatureFlags.VANILLA_SET, "funnelmc",
				null, null, Collections.emptyMap(), null, Collections.emptyMap(), ServerLinks.EMPTY, Collections.emptyMap(), false);
		this.clientPacketListener = new ClientPacketListener(FunnelMC.mc, this.connection, cookie);
		this.packetTranslatorManager = new JavaPacketTranslatorManager();
	}

	public void processServerToClientPacket(Packet<ClientGamePacketListener> packet) {
		//this is what minecraft does, Connection.channelRead0()V
		packet.handle(this.clientPacketListener);
	}

}
