package me.THEREALWWEFAN231.tunnelmc.translator.packet;

import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.List;

public class GameRulesChangedTranslator extends PacketTranslator<GameRulesChangedPacket> {
    @Override
    public void translate(GameRulesChangedPacket packet) {
        if (MinecraftClient.getInstance().world == null) {
            MinecraftClient.getInstance().execute(() -> onGameRulesChanged(packet.getGameRules()));
        } else {
            onGameRulesChanged(packet.getGameRules());
        }
    }

    @Override
    public Class<?> getPacketClass() {
        return GameRulesChangedPacket.class;
    }

    public static void onGameRulesChanged(List<GameRuleData<?>> gamerules) {
        if (MinecraftClient.getInstance().world == null) {
            return;
        }

        for (GameRuleData<?> gamerule : gamerules) {
            switch (gamerule.getName()) {
                case "dodaylightcycle":
                    MinecraftClient.getInstance().world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(((Boolean) gamerule.getValue()), null);
                    break;
                case "doimmediaterespawn":
                    MinecraftClient.getInstance().player.setShowsDeathScreen(!((Boolean) gamerule.getValue()));
                    break;
            }
        }
    }
}
