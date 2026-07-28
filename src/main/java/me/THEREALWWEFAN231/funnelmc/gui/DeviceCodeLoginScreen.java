package me.THEREALWWEFAN231.funnelmc.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import me.THEREALWWEFAN231.funnelmc.auth.DeviceCodeAuth;
import me.THEREALWWEFAN231.funnelmc.bedrockconnection.Client;

// Shown while the player logs in with their Microsoft/Xbox account via the device code flow -
// requests a short code, tells the player where to enter it, and waits. Client#initialize drives
// this screen's state through the AuthListener callbacks; once auth succeeds this hands off to
// whatever Minecraft's normal packet-driven screen transitions do next (the translated login
// packets flow through the same client packet listener a real server connection would use).
@Environment(EnvType.CLIENT)
public class DeviceCodeLoginScreen extends Screen implements Client.AuthListener {

	private final Screen parent;
	private Button copyCodeButton;

	private String statusLine = "Requesting a login code from Microsoft...";
	private String codeLine = "";
	private DeviceCodeAuth.DeviceCodeInfo deviceCodeInfo;

	public DeviceCodeLoginScreen(Screen parent) {
		super(Component.literal("Log in with Microsoft"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.copyCodeButton = this.addRenderableWidget(Button.builder(Component.literal("Copy Code"), button -> {
			if (this.deviceCodeInfo != null) {
				this.minecraft.keyboardHandler.setClipboard(this.deviceCodeInfo.userCode);
			}
		}).pos(this.width / 2 - 102, this.height / 2 + 20).size(204, 20).build());
		this.copyCodeButton.active = false;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreenAndShow(this.parent))
				.pos(this.width / 2 - 102, this.height / 2 + 45).size(204, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
		graphics.centeredText(this.font, Component.literal(this.statusLine), this.width / 2, this.height / 2 - 30, 0xCCCCCC);
		if (!this.codeLine.isEmpty()) {
			graphics.centeredText(this.font, Component.literal(this.codeLine), this.width / 2, this.height / 2 - 10, 0xFFFF55);
		}
	}

	@Override
	public void onDeviceCode(DeviceCodeAuth.DeviceCodeInfo deviceCodeInfo) {
		this.deviceCodeInfo = deviceCodeInfo;
		this.statusLine = "Go to " + deviceCodeInfo.verificationUri + " and enter the code below:";
		this.codeLine = deviceCodeInfo.userCode;
		this.copyCodeButton.active = true;
	}

	@Override
	public void onAuthComplete() {
		this.statusLine = "Logged in! Connecting to the server...";
		this.codeLine = "";
		this.copyCodeButton.active = false;
	}

	@Override
	public void onAuthFailed(Exception e) {
		this.statusLine = "Login failed: " + e.getMessage();
		this.codeLine = "";
		this.copyCodeButton.active = false;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent);
	}

}
