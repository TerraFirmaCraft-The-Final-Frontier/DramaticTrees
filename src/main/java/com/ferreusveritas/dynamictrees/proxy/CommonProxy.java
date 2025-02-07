package com.ferreusveritas.dynamictrees.proxy;


import com.ferreusveritas.dynamictrees.*;
import com.ferreusveritas.dynamictrees.api.TreeRegistry;
import com.ferreusveritas.dynamictrees.api.WorldGenRegistry;
import com.ferreusveritas.dynamictrees.api.treedata.ILeavesProperties;
import com.ferreusveritas.dynamictrees.blocks.LeavesPaging;
import com.ferreusveritas.dynamictrees.blocks.LeavesPropertiesJson;
import com.ferreusveritas.dynamictrees.blocks.MimicProperty;
import com.ferreusveritas.dynamictrees.cells.CellKits;
import com.ferreusveritas.dynamictrees.event.*;
import com.ferreusveritas.dynamictrees.growthlogic.GrowthLogicKits;
import com.ferreusveritas.dynamictrees.worldgen.TreeGenCancelEventHandler;
import com.ferreusveritas.dynamictrees.worldgen.TreeGenerator;
import com.ferreusveritas.dynamictrees.worldgen.WorldGeneratorTrees;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CommonProxy {

	public void registerTileEntities() {
	}

	public void preInit(FMLPreInitializationEvent event) {
		ModConfigs.preInit(event);//Naturally this comes first so we can react to settings
		CellKits.preInit();
		GrowthLogicKits.preInit();
		TreeGenerator.preInit();//Create the generator

		ModTileEntities.preInit();

		ModBlocks.preInit();
		ModItems.preInit();
		ModTrees.preInit();

		registerCommonEventHandlers();
	}

	public void init(FMLInitializationEvent event) {
		LeavesPropertiesJson.resolveAll();
	}

	public void postInit() {
		WorldGenRegistry.populateDataBase();
		ModTrees.setupExtraSoils();
		MimicProperty.setupSoilFlags();
	}

	public void cleanUp() {
		LeavesPropertiesJson.cleanUp();
		LeavesPaging.cleanUp();
		TreeRegistry.cleanupCellKit();
		TreeRegistry.cleanupGrowthLogicKit();
	}

	public void registerModels() {
	}

	public void registerColorHandlers() {
	}

	public void registerCommonEventHandlers() {
		//Common Events.. unused at the moment
		MinecraftForge.EVENT_BUS.register(new CommonEventHandler());
		if (ModConfigs.worldGen) {
			MinecraftForge.EVENT_BUS.register(new DropEventHandler());
		}

		if (Loader.isModLoaded("fastleafdecay")) {
			MinecraftForge.EVENT_BUS.register(new LeafUpdateEventHandler());
		}

		//An event for dealing with Vanilla Saplings
		if (ModConfigs.replaceVanillaSapling) {
			VanillaSaplingEventHandler handler = new VanillaSaplingEventHandler();
			MinecraftForge.EVENT_BUS.register(handler);
			MinecraftForge.TERRAIN_GEN_BUS.register(handler);
		}

		//Conveniently accessible disaster(Optional World Generation)
		if (WorldGenRegistry.isWorldGenEnabled()) {
			GameRegistry.registerWorldGenerator(new WorldGeneratorTrees(), 20);
			MinecraftForge.TERRAIN_GEN_BUS.register(new TreeGenCancelEventHandler());
			MinecraftForge.EVENT_BUS.register(new PoissonDiscEventHandler());
		}
	}

	public int getFoliageColor(ILeavesProperties leavesProperties, World world, IBlockState blockState, BlockPos pos) {
		return 0x00FF00FF;//Magenta shading as error indicator
	}

	///////////////////////////////////////////
	// PARTICLES
	///////////////////////////////////////////

	public void addDustParticle(World world, double fx, double fy, double fz, double mx, double my, double mz, IBlockState blockState, float r, float g, float b) {
	}

	/**
	 * Not strictly necessary. But adds a little more isolation to the server for particle effects
	 */
	public void spawnParticle(World world, EnumParticleTypes particleType, double x, double y, double z, double mx, double my, double mz) {
	}

	public void crushLeavesBlock(World world, BlockPos pos, IBlockState blockState, Entity entity) {
	}

}
