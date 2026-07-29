package me.THEREALWWEFAN231.funnelmc.gui;

import java.util.ArrayList;
import java.util.List;

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
import me.THEREALWWEFAN231.funnelmc.nethernet.PlayFabAuth;
import me.THEREALWWEFAN231.funnelmc.nethernet.SignalingClient;

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

	// This world can only be reached over NetherNet (WebRTC), which FunnelMC doesn't have a data
	// channel/ICE/DTLS/SCTP implementation for yet - actually joining isn't possible from this build.
	// What this DOES verify is the full auth chain leading up to that: Xbox Live -> PlayFab ->
	// franchise MCToken -> signaling websocket -> real STUN/TURN Credentials from Microsoft's
	// server. If this succeeds, the auth side of NetherNet is solid and the only remaining work is
	// the WebRTC transport itself. If it fails, the log will show exactly which step rejected us.
	private void probeNetherNet(XboxLiveApi.Friend friend, long netherNetId) {
		new Thread(() -> {
			try {
				String gameVersion = Client.instance.bedrockCodec.getMinecraftVersion();
				logger.warn("[NetherNetDiag] Starting probe for {}'s session (netherNetId={}, gameVersion={})", friend.gamertag, netherNetId, gameVersion);

				NetherNetDiscovery.DiscoveryResult discovery = NetherNetDiscovery.discover(gameVersion);
				logger.warn("[NetherNetDiag] Discovery: authServiceUri={} playFabTitleId={} signalingServiceUri={}",
						discovery.auth.serviceUri, discovery.auth.playFabTitleId, discovery.signaling.serviceUri);

				String playFabXboxToken = Client.instance.cachedAuth.getXboxTokenForRelyingParty("rp://playfabapi.com/");
				PlayFabAuth.LoginResult playFabLogin = PlayFabAuth.login(discovery.auth.playFabTitleId, playFabXboxToken);
				logger.warn("[NetherNetDiag] PlayFab login succeeded, playFabId={}", playFabLogin.playFabId);

				FranchiseAuth.Token mcToken = FranchiseAuth.startSession(discovery.auth.serviceUri, discovery.auth.playFabTitleId, playFabLogin.sessionTicket, gameVersion);
				logger.warn("[NetherNetDiag] Franchise session/start succeeded, treatments={}", mcToken.treatments);

				String credentials = SignalingClient.connectAndWaitForCredentials(discovery.signaling.serviceUri, mcToken.authorizationHeader, 15);
				logger.warn("[NetherNetDiag] Received signaling Credentials - auth chain works end to end: {}", credentials);

				this.minecraft.execute(() -> this.statusLine = "NetherNet auth chain works! (data channel not implemented yet - see funnelmc.log)");
			} catch (Exception e) {
				logger.error("[NetherNetDiag] Probe failed", e);
				this.minecraft.execute(() -> this.statusLine = "NetherNet probe failed: " + e.getMessage());
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
