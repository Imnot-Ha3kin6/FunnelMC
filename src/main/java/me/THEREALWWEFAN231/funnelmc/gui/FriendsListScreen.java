package me.THEREALWWEFAN231.funnelmc.gui;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import me.THEREALWWEFAN231.funnelmc.auth.XboxLiveApi;
import me.THEREALWWEFAN231.funnelmc.bedrockconnection.Client;

// Lists the player's Xbox friends who are currently active in Minecraft and lets them try to join
// one. Reuses the login the player already did via MicrosoftLoginScreen (cached on Client.instance)
// before they could reach this screen, instead of logging in again.
// The friend-lookup/join part (XboxLiveApi's session directory calls) is the one piece of this
// mod that hasn't been tested against a real account - if it doesn't find anything, or fails,
// that's the most likely place something's still off.
@Environment(EnvType.CLIENT)
public class FriendsListScreen extends Screen {

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
				e.printStackTrace();
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

				this.minecraft.execute(() -> Client.instance.connectWithExistingAuth(session.hostIp, session.hostPort, Client.instance.cachedAuth, Client.instance.cachedOnlineChainData));
			} catch (Exception e) {
				e.printStackTrace();
				this.minecraft.execute(() -> this.statusLine = "Couldn't join " + friend.gamertag + ": " + e.getMessage());
			}
		}, "FunnelMC-Friends-Join").start();
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
