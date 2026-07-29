package me.THEREALWWEFAN231.funnelmc.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.darkmagician6.eventapi.EventManager;

import me.THEREALWWEFAN231.funnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.funnelmc.events.EventPlayerTick;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;

@Mixin(LocalPlayer.class)
public class MixinClientPlayerEntity {

	private double preTickX;
	private double preTickY;
	private double preTickZ;

	// Bedrock is the real ground truth here - the server resolves gravity/collision itself and
	// tells us the result via MovePlayerPacket/PlayerAuthInputPacket echoes (see
	// MovePlayerPacketTranslator), not via us running our own physics against a block translation
	// that's an approximation at best. But LocalPlayer.tick() runs full vanilla physics
	// unconditionally (gravity, collision, onGround) every tick regardless of that, so the local
	// player was falling on its own between server corrections and then getting yanked back once
	// the drift crossed the correction threshold - a sawtooth of free-fall-through-terrain followed
	// by a snap, which is exactly what the logs showed even once block translation was fixed.
	// Capture position before vanilla physics runs and restore it after, so local physics never
	// actually moves the player - only a real server echo does.
	@Inject(method = "tick", at = @At("HEAD"))
	private void captureServerAuthoritativePreTickPosition(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		this.preTickX = self.getX();
		this.preTickY = self.getY();
		this.preTickZ = self.getZ();
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V"))
	public void tick(CallbackInfo callbackInfo) {
		EventManager.call(new EventPlayerTick());
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void suppressLocalPhysicsUnderServerAuthority(CallbackInfo ci) {
		if (Client.instance.movementMode == AuthoritativeMovementMode.CLIENT) {
			return;
		}

		LocalPlayer self = (LocalPlayer) (Object) this;
		self.setPos(this.preTickX, this.preTickY, this.preTickZ);
		self.setDeltaMovement(Vec3.ZERO);
	}

}
