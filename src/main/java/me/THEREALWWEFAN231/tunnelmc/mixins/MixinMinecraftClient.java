package me.THEREALWWEFAN231.tunnelmc.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.cloudburstmc.protocol.bedrock.packet.InteractPacket;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class MixinMinecraftClient {

	@Inject(method = "<init>", at = @At("RETURN"))
	public void init(CallbackInfo callback) {
		TunnelMC.instance.initialize();
	}

	//inventory opened, I could have swore there was some packet for this(that could be translated) I can't find it, I am so confused, found it!!! ClientCommandC2SPacket ClientCommandC2SPacket.Mode.OPEN_INVENTORY, packet might only be sent when the player is riding an entity/
	@Inject(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreenAndShow(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 1))
	private void handleInputEvents(CallbackInfo callbackInfo) {
		if (Client.instance.isConnectionOpen()) {

			InteractPacket interactPacket = new InteractPacket();
			interactPacket.setAction(InteractPacket.Action.OPEN_INVENTORY);
			interactPacket.setRuntimeEntityId(TunnelMC.mc.player.getId());

			Client.instance.sendPacket(interactPacket);
		}
	}

}
