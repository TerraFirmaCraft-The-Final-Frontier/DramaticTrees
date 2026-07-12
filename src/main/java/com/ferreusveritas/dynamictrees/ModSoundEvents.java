package com.ferreusveritas.dynamictrees;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = ModConstants.MODID)
public class ModSoundEvents {
	public static final SoundEvent FALLING_TREE_BIG_START = createSoundEvent("falling_tree_big_start");
	public static final SoundEvent FALLING_TREE_BIG_END = createSoundEvent("falling_tree_big_end");
	public static final SoundEvent FALLING_TREE_MEDIUM_START = createSoundEvent("falling_tree_medium_start");
	public static final SoundEvent FALLING_TREE_MEDIUM_END = createSoundEvent("falling_tree_medium_end");
	public static final SoundEvent FALLING_TREE_SMALL_END = createSoundEvent("falling_tree_small_end");
	public static final SoundEvent FALLING_TREE_SMALL_END_BARE = createSoundEvent("falling_tree_small_end_bare");
	public static final SoundEvent FALLING_TREE_HIT_WATER = createSoundEvent("falling_tree_hit_water");
	public static final SoundEvent FALLING_TREE_SMALL_HIT_WATER = createSoundEvent("falling_tree_small_hit_water");
	public static final SoundEvent FALLING_TREE_FUNGUS_START = createSoundEvent("falling_tree_fungus_start");
	public static final SoundEvent FALLING_TREE_FUNGUS_END = createSoundEvent("falling_tree_fungus_end");
	public static final SoundEvent FALLING_TREE_FUNGUS_SMALL_END = createSoundEvent("falling_tree_fungus_small_end");

	private static SoundEvent createSoundEvent(String path) {
		ResourceLocation rl = new ResourceLocation(ModConstants.MODID, path);
		return new SoundEvent(rl).setRegistryName(rl);
	}

	@SubscribeEvent
	public static void register(RegistryEvent.Register<SoundEvent> event) {
		event.getRegistry().registerAll(
			FALLING_TREE_BIG_START,
			FALLING_TREE_BIG_END,
			FALLING_TREE_MEDIUM_START,
			FALLING_TREE_MEDIUM_END,
			FALLING_TREE_SMALL_END,
			FALLING_TREE_SMALL_END_BARE,
			FALLING_TREE_HIT_WATER,
			FALLING_TREE_SMALL_HIT_WATER,
			FALLING_TREE_FUNGUS_START,
			FALLING_TREE_FUNGUS_END,
			FALLING_TREE_FUNGUS_SMALL_END
		);
	}
}
