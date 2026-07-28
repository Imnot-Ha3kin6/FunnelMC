package me.THEREALWWEFAN231.funnelmc;

import me.THEREALWWEFAN231.funnelmc.translator.EntityTranslator;
import me.THEREALWWEFAN231.funnelmc.translator.PacketTranslatorManager;
import me.THEREALWWEFAN231.funnelmc.translator.blockentity.BlockEntityRegistry;
import me.THEREALWWEFAN231.funnelmc.translator.blockentity.BlockEntityTranslator;
import me.THEREALWWEFAN231.funnelmc.translator.blockstate.BlockStateTranslator;
import me.THEREALWWEFAN231.funnelmc.translator.container.screenhandler.ScreenHandlerTranslatorManager;
import me.THEREALWWEFAN231.funnelmc.translator.enchantment.EnchantmentTranslator;
import me.THEREALWWEFAN231.funnelmc.translator.item.ItemTranslator;
import me.THEREALWWEFAN231.funnelmc.javaconnection.packet.movement.PlayerAuthInputSender;
import me.THEREALWWEFAN231.funnelmc.utils.FileManagement;
import me.THEREALWWEFAN231.funnelmc.utils.FunnelLogSetup;
import net.minecraft.client.Minecraft;

public class FunnelMC {

	public static FunnelMC instance = new FunnelMC();
	public static Minecraft mc = Minecraft.getInstance();

	public FileManagement fileManagement;
	public PacketTranslatorManager packetTranslatorManager;

	public void initialize() {
		FunnelLogSetup.install();

		this.fileManagement = new FileManagement();
		this.packetTranslatorManager = new PacketTranslatorManager();

		BlockEntityRegistry.load();
		BlockStateTranslator.load();
		EntityTranslator.load();
		ItemTranslator.load();
		EnchantmentTranslator.load();
		ScreenHandlerTranslatorManager.load();
		new PlayerAuthInputSender();
	}

}
