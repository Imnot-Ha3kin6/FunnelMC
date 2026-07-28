package me.THEREALWWEFAN231.funnelmc;

import me.THEREALWWEFAN231.funnelmc.translator.EntityTranslator;
import me.THEREALWWEFAN231.funnelmc.translator.PacketTranslatorManager;
import me.THEREALWWEFAN231.funnelmc.translator.blockentity.BlockEntityRegistry;
import me.THEREALWWEFAN231.funnelmc.translator.blockentity.BlockEntityTranslator;
import me.THEREALWWEFAN231.funnelmc.translator.blockstate.BlockStateTranslator;
import me.THEREALWWEFAN231.funnelmc.translator.container.screenhandler.ScreenHandlerTranslatorManager;
import me.THEREALWWEFAN231.funnelmc.translator.enchantment.EnchantmentTranslator;
import me.THEREALWWEFAN231.funnelmc.translator.item.ItemTranslator;
import me.THEREALWWEFAN231.funnelmc.utils.FileManagement;
import net.minecraft.client.Minecraft;

public class FunnelMC {

	public static FunnelMC instance = new FunnelMC();
	public static Minecraft mc = Minecraft.getInstance();

	public FileManagement fileManagement;
	public PacketTranslatorManager packetTranslatorManager;

	public void initialize() {
		this.fileManagement = new FileManagement();
		this.packetTranslatorManager = new PacketTranslatorManager();

		BlockEntityRegistry.load();
		BlockStateTranslator.load();
		EntityTranslator.load();
		ItemTranslator.load();
		EnchantmentTranslator.load();
		ScreenHandlerTranslatorManager.load();
	}

}
