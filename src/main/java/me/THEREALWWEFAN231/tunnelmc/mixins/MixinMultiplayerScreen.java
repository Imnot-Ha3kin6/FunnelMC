package me.THEREALWWEFAN231.tunnelmc.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.gui.BedrockConnectionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

@Mixin(JoinMultiplayerScreen.class)
public class MixinMultiplayerScreen extends Screen {

	protected MixinMultiplayerScreen(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At(value = "RETURN"))
	public void init(CallbackInfo callback) {
		this.addRenderableWidget(Button.builder(Component.literal("Connect To Bedrock Server"), (button) -> {
			TunnelMC.mc.setScreenAndShow(new BedrockConnectionScreen(this));
		}).pos(5, 5).size(150, 20).build());
	}

}
