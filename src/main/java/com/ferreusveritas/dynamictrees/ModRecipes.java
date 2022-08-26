package com.ferreusveritas.dynamictrees;

import com.ferreusveritas.dynamictrees.api.TreeRegistry;
import com.ferreusveritas.dynamictrees.items.Seed;
import com.ferreusveritas.dynamictrees.trees.Species;
import net.minecraft.block.BlockPlanks;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = ModConstants.MODID)
public class ModRecipes {
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void register(RegistryEvent.Register<IRecipe> event) {
		IForgeRegistry<IRecipe> registry = event.getRegistry();
		
		ModItems.dendroPotion.registerRecipes(registry);
		
		//Create a seed <-> sapling exchange for the 6 vanilla tree types
		for (BlockPlanks.EnumType woodType : BlockPlanks.EnumType.values()) {
			Species species = TreeRegistry.findSpecies(new ResourceLocation(ModConstants.MODID, woodType.getName().replace("_", "")));
			ItemStack saplingStack = new ItemStack(Blocks.SAPLING, 1, woodType.getMetadata());
			ItemStack seedStack = species.getSeedStack(1);
		}
	}
	/**
	 * Convenience function to do all of the work of creating the recipes for a fruit tree.
	 * 
	 * @param saplingStack
	 * @param seedStack
	 * @param fruitStack
	 * @param seedIsSapling
	 * @param suffix
	 * @param species
	 * @param requiresBonemeal
	 */
	
	/** 
	 * Create a recipe that handles fruit to seed conversion.
	 *
	 * 
	 * @param seedStack The itemStack containing the seed item used to grow the tree
	 * @param fruitStack The itemStack for the fruit item such as a fig or cherries.
	 * @param species The Species of the fruit bearing tree
	 * @param requiresBonemeal True if the recipe also requires bonemeal
	 */
	private static void createFruitToSeedRecipe(ItemStack seedStack, ItemStack fruitStack, ResourceLocation species, boolean requiresBonemeal) {
		if (fruitStack != null && !fruitStack.isEmpty()) {
			ResourceLocation seedFromFruit = new ResourceLocation(species.getResourceDomain(), species.getResourcePath() + "seedfromfruit");
			Ingredient fruit = Ingredient.fromStacks(fruitStack);
			Ingredient bonemeal = Ingredient.fromStacks(new ItemStack(Items.DYE, 1, 15));
		}
	}
	
}
