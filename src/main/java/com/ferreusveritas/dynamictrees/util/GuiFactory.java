package com.ferreusveritas.dynamictrees.util;

import com.ferreusveritas.dynamictrees.ModConfigs;
import com.ferreusveritas.dynamictrees.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GuiFactory implements IModGuiFactory {

	@Override
	public void initialize(Minecraft minecraftInstance) {

	}

	@Override
	public boolean hasConfigGui() {
		return true;
	}

	@Override
	public GuiScreen createConfigGui(GuiScreen parentScreen) {
		return new ConfigGui(parentScreen);
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}

	public static class ConfigGui extends GuiConfig {

		public ConfigGui(GuiScreen parentScreen) {
			super(parentScreen, getConfigElements(), ModConstants.MODID, false, false, ModConstants.NAME);
		}

		private static List<IConfigElement> getConfigElements() {
			return ModConfigs.config.getCategoryNames().stream()
				.map(e -> new ConfigElement(ModConfigs.config.getCategory(e)))
				.collect(Collectors.toList());
		}

	}
}
