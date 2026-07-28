package me.THEREALWWEFAN231.funnelmc.translator.container.screenhandler;

import java.util.HashMap;

import me.THEREALWWEFAN231.funnelmc.bedrockconnection.caches.container.BedrockContainer;
import me.THEREALWWEFAN231.funnelmc.translator.container.screenhandler.translators.GenericContainerScreenHandlerTranslator;
import me.THEREALWWEFAN231.funnelmc.translator.container.screenhandler.translators.PlayerScreenHandlerTranslator;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class ScreenHandlerTranslatorManager {
	
	private static final HashMap<Class<? extends AbstractContainerMenu>, ScreenHandlerTranslator<?>> REGISTRY = new HashMap<Class<? extends AbstractContainerMenu>, ScreenHandlerTranslator<?>>();
	
	public static void load() {
		ScreenHandlerTranslatorManager.add(new PlayerScreenHandlerTranslator());
		ScreenHandlerTranslatorManager.add(new GenericContainerScreenHandlerTranslator());
	}
	
	private static void add(ScreenHandlerTranslator<?> translator) {
		ScreenHandlerTranslatorManager.REGISTRY.put(translator.getScreenHandlerClass(), translator);
	}
	
	private static ScreenHandlerTranslator<AbstractContainerMenu> getTranslator(AbstractContainerMenu screenHandler){
		Class<? extends AbstractContainerMenu> screenHandlerClass = screenHandler.getClass();
		ScreenHandlerTranslator<AbstractContainerMenu> translator = (ScreenHandlerTranslator<AbstractContainerMenu>) ScreenHandlerTranslatorManager.REGISTRY.get(screenHandlerClass);
		
		if(translator == null) {
			System.out.println("No screen handler found for " + screenHandlerClass);
			return null;
		}
		
		return translator;
	}
	
	public static BedrockContainer getBedrockContainerFromJava(AbstractContainerMenu javaContainer, int javaSlotId) {
		return ScreenHandlerTranslatorManager.getTranslator(javaContainer).getBedrockContainerFromJava(javaContainer, javaSlotId);
	}
	
	public static int getJavaSlotFromBedrockContainer(AbstractContainerMenu javaContainer, BedrockContainer bedrockContainer, int bedrockSlotId) {
		return ScreenHandlerTranslatorManager.getTranslator(javaContainer).getJavaSlotFromBedrockContainer(javaContainer, bedrockContainer, bedrockSlotId);
	}
	
	public static int getBedrockSlotFromJavaContainer(AbstractContainerMenu javaContainer, int javaSlotId, BedrockContainer bedrockContainer) {
		return ScreenHandlerTranslatorManager.getTranslator(javaContainer).getBedrockSlotFromJavaContainer(javaContainer, javaSlotId, bedrockContainer);
	}
}
