package me.THEREALWWEFAN231.funnelmc.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import me.THEREALWWEFAN231.funnelmc.auth.XboxLiveApi;
import me.THEREALWWEFAN231.funnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.funnelmc.nethernet.FranchiseAuth;
import me.THEREALWWEFAN231.funnelmc.nethernet.NetherNetDiscovery;
import me.THEREALWWEFAN231.funnelmc.nethernet.NetherNetSignal;
import me.THEREALWWEFAN231.funnelmc.nethernet.NetherNetTransport;
import me.THEREALWWEFAN231.funnelmc.nethernet.PlayFabAuth;
import me.THEREALWWEFAN231.funnelmc.nethernet.SignalingConnection;

// Lists the player's Xbox friends who are currently active in Minecraft and lets them try to join
// one. Reuses the login the player already did via MicrosoftLoginScreen (cached on Client.instance)
// before they could reach this screen, instead of logging in again.
// Sessions with a direct IP/port connect the same way "Connect to Bedrock Server" does. Sessions
// that only offer NetherNet (WebRTC) can't actually be joined yet - see probeNetherNet's comment.
@Environment(EnvType.CLIENT)
public class FriendsListScreen extends Screen {

	private static final Logger logger = LogManager.getLogger(FriendsListScreen.class);

	private final Screen parent;
	private String statusLine = "Looking for friends playing Minecraft...";

	public FriendsListScreen(Screen parent) {
		super(Component.literal("Bedrock Friends"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreenAndShow(this.parent))
				.pos(this.width / 2 - 102, this.height - 40).size(204, 20).build());

		if (!Client.instance.hasCachedLogin()) {
			this.statusLine = "Not logged in - go back and reconnect to sign in.";
			return;
		}

		this.loadFriends();
	}

	private void loadFriends() {
		String xboxLiveAuthorizationHeader = Client.instance.cachedXboxLiveAuthorizationHeader;

		new Thread(() -> {
			try {
				List<XboxLiveApi.Friend> friends = XboxLiveApi.getFriends(xboxLiveAuthorizationHeader);

				List<String> xuids = new ArrayList<>();
				for (XboxLiveApi.Friend friend : friends) {
					xuids.add(friend.xuid);
				}

				List<String> activeXuids = XboxLiveApi.getXuidsActiveInMinecraft(xuids, xboxLiveAuthorizationHeader);

				List<XboxLiveApi.Friend> activeFriends = new ArrayList<>();
				for (XboxLiveApi.Friend friend : friends) {
					if (activeXuids.contains(friend.xuid)) {
						activeFriends.add(friend);
					}
				}

				this.minecraft.execute(() -> this.showFriends(activeFriends));
			} catch (Exception e) {
				logger.error("[FriendsDiag] Failed to load friends", e);
				this.minecraft.execute(() -> this.statusLine = "Failed to load friends: " + e.getMessage());
			}
		}, "FunnelMC-Friends-Load").start();
	}

	private void showFriends(List<XboxLiveApi.Friend> activeFriends) {
		if (activeFriends.isEmpty()) {
			this.statusLine = "None of your friends are currently playing Minecraft.";
			return;
		}

		this.statusLine = "Friends currently playing Minecraft:";

		int y = this.height / 2 - 40;
		for (XboxLiveApi.Friend friend : activeFriends) {
			this.addRenderableWidget(Button.builder(Component.literal(friend.gamertag), button -> this.tryJoin(friend))
					.pos(this.width / 2 - 102, y).size(204, 20).build());
			y += 24;
		}
	}

	private void tryJoin(XboxLiveApi.Friend friend) {
		this.statusLine = "Looking up " + friend.gamertag + "'s world...";
		String xboxLiveAuthorizationHeader = Client.instance.cachedXboxLiveAuthorizationHeader;

		new Thread(() -> {
			try {
				XboxLiveApi.JoinableSession session = XboxLiveApi.findJoinableSession(friend.xuid, xboxLiveAuthorizationHeader);

				if (session == null) {
					this.minecraft.execute(() -> this.statusLine = friend.gamertag + " doesn't have a joinable world right now.");
					return;
				}

				if (session.isDirectIp()) {
					this.minecraft.execute(() -> Client.instance.connectWithExistingAuth(session.hostIp, session.hostPort, Client.instance.cachedAuth, Client.instance.cachedOnlineChainData));
					return;
				}

				if (session.netherNetId == null) {
					this.minecraft.execute(() -> this.statusLine = friend.gamertag + "'s session has no usable connection info.");
					return;
				}

				this.minecraft.execute(() -> this.statusLine = friend.gamertag + "'s world uses NetherNet - probing signaling connection (see funnelmc.log)...");
				this.probeNetherNet(friend, session.netherNetId);
			} catch (Exception e) {
				logger.error("[FriendsDiag] Couldn't join {}", friend.gamertag, e);
				this.minecraft.execute(() -> this.statusLine = "Couldn't join " + friend.gamertag + ": " + e.getMessage());
			}
		}, "FunnelMC-Friends-Join").start();
	}

	// This world can only be reached over NetherNet (WebRTC). The auth chain (Xbox Live -> PlayFab
	// -> franchise MCToken -> signaling websocket -> real STUN/TURN Credentials) was already proven
	// working end to end in an earlier build. This drives the actual WebRTC negotiation on top of
	// it: open a persistent signaling connection, wait for Credentials, then have NetherNetTransport
	// create the data channels, send our SDP offer, and apply the remote's answer/ICE candidates as
	// they arrive. Success here means the data channel reaches OPEN - actually routing Bedrock
	// packets over it (replacing the RakNet/UDP pipeline for this connection) is the next step.
	private void probeNetherNet(XboxLiveApi.Friend friend, long netherNetId) {
		new Thread(() -> {
			SignalingConnection signaling = null;
			NetherNetTransport transport = null;
			try {
				String gameVersion = Client.instance.bedrockCodec.getMinecraftVersion();
				logger.warn("[NetherNetDiag] Starting probe for {}'s session (netherNetId={}, gameVersion={})", friend.gamertag, netherNetId, gameVersion);

				NetherNetDiscovery.DiscoveryResult discovery = NetherNetDiscovery.discover(gameVersion);
				String playFabXboxToken = Client.instance.cachedAuth.getXboxTokenForRelyingParty("rp://playfabapi.com/");
				PlayFabAuth.LoginResult playFabLogin = PlayFabAuth.login(discovery.auth.playFabTitleId, playFabXboxToken);
				FranchiseAuth.Token mcToken = FranchiseAuth.startSession(discovery.auth.serviceUri, discovery.auth.playFabTitleId, playFabLogin.sessionTicket, gameVersion);
				logger.warn("[NetherNetDiag] Auth chain ready, opening signaling connection");

				String targetNetworkId = Long.toUnsignedString(netherNetId);
				AtomicReference<NetherNetTransport> transportRef = new AtomicReference<>();

				signaling = SignalingConnection.connect(discovery.signaling.serviceUri, mcToken.authorizationHeader, new SignalingConnection.Listener() {
					@Override
					public void onCredentials(String credentialsJson) {
						logger.warn("[NetherNetDiag] Signaling credentials received");
					}

					@Override
					public void onSignal(NetherNetSignal signal, String fromNetworkId) {
						NetherNetTransport t = transportRef.get();
						if (t != null && targetNetworkId.equals(fromNetworkId)) {
							t.handleSignal(signal);
						} else {
							logger.warn("[NetherNetDiag] Ignoring signal from unexpected/unready source: from={} type={}", fromNetworkId, signal.type);
						}
					}

					@Override
					public void onClosed(int statusCode, String reason) {
						logger.warn("[NetherNetDiag] Signaling connection closed: {} {}", statusCode, reason);
					}
				});

				String credentialsJson = signaling.awaitCredentials(15);

				transport = new NetherNetTransport(signaling, netherNetId, credentialsJson);
				transportRef.set(transport);
				transport.connect();

				this.minecraft.execute(() -> this.statusLine = "Negotiating WebRTC connection to " + friend.gamertag + "'s world (see funnelmc.log)...");

				transport.whenDataChannelOpen().get(20, TimeUnit.SECONDS);
				logger.warn("[NetherNetDiag] Data channel OPEN - WebRTC transport is fully connected!");
				this.minecraft.execute(() -> this.statusLine = "NetherNet data channel connected! (packet pipeline not wired up yet - see funnelmc.log)");
			} catch (Exception e) {
				logger.error("[NetherNetDiag] Probe failed", e);
				final SignalingConnection signalingToClose = signaling;
				final NetherNetTransport transportToClose = transport;
				this.minecraft.execute(() -> {
					this.statusLine = "NetherNet probe failed: " + e.getMessage();
					if (transportToClose != null) {
						transportToClose.close();
					}
					if (signalingToClose != null) {
						signalingToClose.close();
					}
				});
			}
		}, "FunnelMC-NetherNet-Probe").start();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 80, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.literal(this.statusLine), this.width / 2, this.height / 2 - 60, 0xFFCCCCCC);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent);
	}

}
