package me.THEREALWWEFAN231.tunnelmc.javaconnection.packet;

import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;

public class HandSwingC2SPacketTranslator extends PacketTranslator<ServerboundSwingPacket> {

	@Override
	public void translate(ServerboundSwingPacket packet) {
		AnimatePacket animatePacket = new AnimatePacket();
		animatePacket.setAction(AnimatePacket.Action.SWING_ARM);
		animatePacket.setRuntimeEntityId(TunnelMC.mc.player.getId());
		Client.instance.sendPacket(animatePacket);
	}

	@Override
	public Class<?> getPacketClass() {
		return ServerboundSwingPacket.class;
	}

}
