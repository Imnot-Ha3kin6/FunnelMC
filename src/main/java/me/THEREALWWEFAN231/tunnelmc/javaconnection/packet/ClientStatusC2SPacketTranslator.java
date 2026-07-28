package me.THEREALWWEFAN231.tunnelmc.javaconnection.packet;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket;
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket.State;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

public class ClientStatusC2SPacketTranslator extends PacketTranslator<ServerboundClientCommandPacket> {

	@Override
	public void translate(ServerboundClientCommandPacket packet) {

		switch (packet.getAction()) {
		case PERFORM_RESPAWN:

			//when the player clicks respawn send the client_ready respawn packet
			RespawnPacket respawnPacket = new RespawnPacket();
			respawnPacket.setPosition(Vector3f.ZERO);
			respawnPacket.setState(State.CLIENT_READY);
			respawnPacket.setRuntimeEntityId(TunnelMC.mc.player.getId());

			Client.instance.sendPacket(respawnPacket);

			break;

		default:
			break;
		}

	}

	@Override
	public Class<?> getPacketClass() {
		return ServerboundClientCommandPacket.class;
	}

}
