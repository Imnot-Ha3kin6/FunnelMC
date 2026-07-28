package me.THEREALWWEFAN231.tunnelmc.bedrockconnection;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.cloudburstmc.protocol.bedrock.BedrockClientSession;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v1001.Bedrock_v1001;
import org.cloudburstmc.protocol.bedrock.data.auth.CertificateChainPayload;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockClientInitializer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.auth.Auth;
import me.THEREALWWEFAN231.tunnelmc.auth.SkinData;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.caches.BlockEntityDataCache;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.caches.container.BedrockContainers;
import me.THEREALWWEFAN231.tunnelmc.javaconnection.FakeJavaConnection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.network.chat.Component;

public class Client {

	public static Client instance = new Client();
	private final Logger logger = LogManager.getLogger(ClientBatchHandler.class);
	public BedrockCodec bedrockCodec = Bedrock_v1001.CODEC;
	private String ip;
	private int port;
	private boolean onlineMode;
	public Auth authData;
	public BedrockClientSession bedrockSession;
	public FakeJavaConnection javaConnection;

	public BedrockContainers containers;
	public BlockEntityDataCache blockEntityDataCache;
	public byte openContainerId;

	public void initialize(String ip, int port, boolean onlineMode) {
		this.ip = ip;
		this.port = port;
		this.onlineMode = onlineMode;

		org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
		logger.get().setLevel(Level.DEBUG);

		InetSocketAddress addressToConnect = new InetSocketAddress(ip, port);

		ChannelFuture future = new Bootstrap()
				.channelFactory(RakChannelFactory.client(NioDatagramChannel.class))
				.group(new NioEventLoopGroup())
				.handler(new BedrockClientInitializer() {
					@Override
					protected void initSession(BedrockClientSession session) {
						session.setCodec(Client.this.bedrockCodec);
						session.setPacketHandler(new ClientBatchHandler());
						session.setLogging(false);

						Client.this.onSessionInitialized(session);
					}
				})
				.connect(addressToConnect);

		future.addListener(result -> {
			if (!result.isSuccess()) {
				Minecraft.getInstance().execute(() -> Minecraft.getInstance().disconnect(new DisconnectedScreen(Minecraft.getInstance().gui.screen(), Component.literal("Use Translated Here"), Component.literal(result.cause().getMessage())), false));
			}
		});
	}

	public void onSessionInitialized(BedrockClientSession bedrockSession) {
		this.bedrockSession = bedrockSession;

		try {
			LoginPacket loginPacket = new LoginPacket();

			this.authData = new Auth();
			if (this.onlineMode) {
				loginPacket.setAuthPayload(new CertificateChainPayload(this.authData.getOnlineChainData()));
			} else {
				loginPacket.setAuthPayload(new CertificateChainPayload(this.authData.getOfflineChainData(Minecraft.getInstance().getUser().getName())));
			}

			loginPacket.setProtocolVersion(bedrockSession.getCodec().getProtocolVersion());
			loginPacket.setClientJwt(SkinData.getSkinData(this.ip + ":" + this.port));
			this.sendPacketImmediately(loginPacket);

			this.javaConnection = new FakeJavaConnection();

		} catch (Exception e) {
			//TODO: do something better with this
			e.printStackTrace();
		}
	}

	//when our java player is initialized
	public void onPlayerInitialized() {
		this.containers = new BedrockContainers();
		this.blockEntityDataCache = new BlockEntityDataCache();
		this.openContainerId = 0;
	}

	public boolean isConnectionOpen() {
		return this.bedrockSession != null && this.bedrockSession.isConnected();
	}

	public void sendPacketImmediately(BedrockPacket packet) {
		this.bedrockSession.sendPacketImmediately(packet);
		if (this.bedrockSession.isLogging()) {
			this.logger.info("Outbound {}: {}", this.bedrockSession.getSocketAddress(), packet.getClass().getCanonicalName());
		}
	}

	public void sendPacket(BedrockPacket packet) {
		this.bedrockSession.sendPacket(packet);
		if (this.bedrockSession.isLogging()) {
			this.logger.info("Outbound {}: {}", this.bedrockSession.getSocketAddress(), packet.getClass().getCanonicalName());
		}
	}

}
