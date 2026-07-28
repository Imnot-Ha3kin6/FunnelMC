package me.THEREALWWEFAN231.funnelmc.javaconnection.packet.movement;

import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.data.ClientPlayMode;
import org.cloudburstmc.protocol.bedrock.data.InputInteractionModel;
import org.cloudburstmc.protocol.bedrock.data.InputMode;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;

import me.THEREALWWEFAN231.funnelmc.FunnelMC;
import me.THEREALWWEFAN231.funnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.funnelmc.events.EventPlayerTick;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

// Servers running server-authoritative movement (most current ones, including the vanilla Bedrock
// Dedicated Server by default) expect PlayerAuthInputPacket every tick instead of the legacy
// client-authoritative MovePlayerPacket (which they reject outright - see the movementMode guard in
// PlayerMoveTranslator). This only reports position/rotation - sprint/sneak/jump input flags,
// analog move axes, and delta/motion prediction aren't tracked yet, so server-side movement
// prediction won't be as smooth as a real Bedrock client, but it's enough to stop the server from
// disconnecting us for sending a packet it won't accept.
public class PlayerAuthInputSender {

	private long tick;

	public PlayerAuthInputSender() {
		EventManager.register(this);
	}

	@EventTarget
	public void onEvent(EventPlayerTick event) {
		if (Client.instance.movementMode == AuthoritativeMovementMode.CLIENT) {
			return;
		}

		if (!Client.instance.isConnectionOpen() || FunnelMC.mc.player == null) {
			return;
		}

		Vec3 lookAngle = FunnelMC.mc.player.getLookAngle();

		PlayerAuthInputPacket packet = new PlayerAuthInputPacket();
		packet.setPosition(Vector3f.from(FunnelMC.mc.player.getX(), FunnelMC.mc.player.getY() + FunnelMC.mc.player.getEyeHeight(Pose.STANDING), FunnelMC.mc.player.getZ()));
		packet.setRotation(Vector3f.from(FunnelMC.mc.player.getXRot(), FunnelMC.mc.player.getYRot(), FunnelMC.mc.player.getYRot()));
		packet.setMotion(Vector2f.ZERO);
		packet.setInputMode(InputMode.MOUSE);
		packet.setPlayMode(ClientPlayMode.NORMAL);
		packet.setInputInteractionModel(InputInteractionModel.CLASSIC);
		packet.setInteractRotation(Vector2f.ZERO);
		packet.setTick(this.tick++);
		packet.setDelta(Vector3f.ZERO);
		packet.setAnalogMoveVector(Vector2f.ZERO);
		packet.setCameraOrientation(Vector3f.from((float) lookAngle.x, (float) lookAngle.y, (float) lookAngle.z));
		packet.setRawMoveVector(Vector2f.ZERO);

		Client.instance.sendPacket(packet);
	}

}
