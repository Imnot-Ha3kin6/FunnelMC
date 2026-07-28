package me.THEREALWWEFAN231.funnelmc.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.THEREALWWEFAN231.funnelmc.FunnelMC;
import me.THEREALWWEFAN231.funnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.funnelmc.utils.ScaffoldPlace;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
	
	/*@Inject(method = "updateTargetedEntity", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"))
	public void updateTargetedEntity(float tickDelta, CallbackInfo callbackInfo) {
		ScaffoldPlace.setRaycastRsult();
	}*/
	
	@Inject(method = "updateTargetedEntity", at = @At("RETURN"))
	public void updateTargetedEntity(float tickDelta, CallbackInfo callbackInfo) {
		if(FunnelMC.mc.getCameraEntity() == null || !Client.instance.isConnectionOpen()) {
			return;
		}
		ScaffoldPlace.setRaycastResult();
	}
	
}
