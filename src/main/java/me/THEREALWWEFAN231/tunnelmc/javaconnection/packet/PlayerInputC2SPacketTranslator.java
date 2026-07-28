package me.THEREALWWEFAN231.tunnelmc.javaconnection.packet;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;

// Sneak (shift) state used to arrive as ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY/
// RELEASE_SHIFT_KEY - that's gone in modern MC, replaced by the shift flag on this
// per-tick input state packet, so we track the last known state ourselves.
public class PlayerInputC2SPacketTranslator extends PacketTranslator<ServerboundPlayerInputPacket> {

	private boolean lastShift = false;

	@Override
	public void translate(ServerboundPlayerInputPacket packet) {
		boolean shift = packet.input().shift();
		if (shift == this.lastShift) {
			return;
		}
		this.lastShift = shift;

		PlayerActionPacket playerActionPacket = new PlayerActionPacket();
		playerActionPacket.setRuntimeEntityId(TunnelMC.mc.player.getId());
		playerActionPacket.setAction(shift ? PlayerActionType.START_SNEAK : PlayerActionType.STOP_SNEAK);
		playerActionPacket.setBlockPosition(Vector3i.ZERO);

		Client.instance.sendPacket(playerActionPacket);
	}

	@Override
	public Class<?> getPacketClass() {
		return ServerboundPlayerInputPacket.class;
	}

}
