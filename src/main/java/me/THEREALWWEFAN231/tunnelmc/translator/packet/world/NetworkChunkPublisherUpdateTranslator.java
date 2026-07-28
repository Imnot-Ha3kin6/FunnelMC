package me.THEREALWWEFAN231.tunnelmc.translator.packet.world;

import org.cloudburstmc.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;

import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;

public class NetworkChunkPublisherUpdateTranslator extends PacketTranslator<NetworkChunkPublisherUpdatePacket> {

    @Override
    public void translate(NetworkChunkPublisherUpdatePacket packet) {
        ClientboundSetChunkCacheCenterPacket renderDistanceCenterPacket = new ClientboundSetChunkCacheCenterPacket(
                packet.getPosition().getX() >> 4, packet.getPosition().getZ() >> 4);
        Client.instance.javaConnection.processServerToClientPacket(renderDistanceCenterPacket);
    }

    @Override
    public Class<?> getPacketClass() {
        return NetworkChunkPublisherUpdatePacket.class;
    }
}
