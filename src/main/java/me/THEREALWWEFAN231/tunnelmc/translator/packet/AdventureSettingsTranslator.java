package me.THEREALWWEFAN231.tunnelmc.translator.packet;

import org.cloudburstmc.protocol.bedrock.data.AdventureSetting;
import org.cloudburstmc.protocol.bedrock.packet.AdventureSettingsPacket;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;

public class AdventureSettingsTranslator extends PacketTranslator<AdventureSettingsPacket> {
    @Override
    public void translate(AdventureSettingsPacket packet) {
        PlayerAbilities abilities = new PlayerAbilities();
        abilities.allowFlying = packet.getSettings().contains(AdventureSetting.MAY_FLY);
        abilities.allowModifyWorld = packet.getSettings().contains(AdventureSetting.BUILD);
        abilities.flying = packet.getSettings().contains(AdventureSetting.FLYING);
        abilities.invulnerable = false;

        PlayerAbilitiesS2CPacket abilitiesPacket = new PlayerAbilitiesS2CPacket(abilities);
        Client.instance.javaConnection.processServerToClientPacket(abilitiesPacket);
    }

    @Override
    public Class<?> getPacketClass() {
        return AdventureSettingsPacket.class;
    }
}
