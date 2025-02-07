package com.ferreusveritas.dynamictrees.trees;

import com.ferreusveritas.dynamictrees.ModBlocks;
import com.ferreusveritas.dynamictrees.ModConfigs;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.items.Seed;
import com.ferreusveritas.dynamictrees.systems.dropcreators.DropCreatorApple;
import com.ferreusveritas.dynamictrees.systems.featuregen.FeatureGenFruit;
import com.ferreusveritas.dynamictrees.systems.featuregen.FeatureGenVine;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.List;
import java.util.Random;

public class TreeOak extends TreeFamilyVanilla {

	public class SpeciesOak extends Species {

		SpeciesOak(TreeFamily treeFamily) {
			super(treeFamily.getName(), treeFamily);

			//Oak trees are about as average as you can get
			setBasicGrowingParameters(0.3f, 12.0f, upProbability, lowestBranchHeight, 0.8f);

			envFactor(Type.COLD, 0.75f);
			envFactor(Type.HOT, 0.50f);
			envFactor(Type.DRY, 0.50f);
			envFactor(Type.FOREST, 1.05f);

			if (ModConfigs.worldGen && !ModConfigs.enableAppleTrees) {//If we've disabled apple trees we still need some way to get apples.
				addDropCreator(new DropCreatorApple());
			}

			setupStandardSeedDropping();
		}

		@Override
		public boolean isBiomePerfect(Biome biome) {
			return isOneOfBiomes(biome, Biomes.FOREST, Biomes.FOREST_HILLS);
		}

		@Override
		public boolean rot(World world, BlockPos pos, int neighborCount, int radius, Random random, boolean rapid) {
			if (super.rot(world, pos, neighborCount, radius, random, rapid)) {
				if (radius > 4 && TreeHelper.isRooty(world.getBlockState(pos.down())) && world.getLightFor(EnumSkyBlock.SKY, pos) < 4) {
					world.setBlockState(pos, random.nextInt(3) == 0 ? ModBlocks.blockStates.redMushroom : ModBlocks.blockStates.brownMushroom);//Change branch to a mushroom
					world.setBlockState(pos.down(), ModBlocks.blockStates.podzol);//Change rooty dirt to Podzol
				}
				return true;
			}

			return false;
		}

	}

	/**
	 * Swamp Oaks are just Oaks with slight growth differences that can generate in water and with vines hanging from
	 * their leaves.
	 */
	public class SpeciesSwampOak extends Species {

		SpeciesSwampOak(TreeFamily treeFamily) {
			super(new ResourceLocation(treeFamily.getName().getResourceDomain(), treeFamily.getName().getResourcePath() + "swamp"), treeFamily);

			setBasicGrowingParameters(0.3f, 12.0f, upProbability, lowestBranchHeight, 0.8f);

			envFactor(Type.COLD, 0.50f);
			envFactor(Type.DRY, 0.50f);

			setupStandardSeedDropping();

			//Add species features
			addGenFeature(new FeatureGenVine().setMaxLength(7).setVerSpread(30).setRayDistance(6).setQuantity(24));//Generate Vines
		}

		@Override
		public boolean isBiomePerfect(Biome biome) {
			return isOneOfBiomes(biome, Biomes.SWAMPLAND);
		}

		//Swamp Oaks are just oaks in a swamp..  So they have the same seeds
		@Override
		public ItemStack getSeedStack(int qty) {
			return getCommonSpecies().getSeedStack(qty);
		}

		//Swamp Oaks are just oaks in a swamp..  So they have the same seeds
		@Override
		public Seed getSeed() {
			return getCommonSpecies().getSeed();
		}

		@Override
		public boolean rot(World world, BlockPos pos, int neighborCount, int radius, Random random, boolean rapid) {
			if (super.rot(world, pos, neighborCount, radius, random, rapid)) {
				if (radius > 4 && TreeHelper.isRooty(world.getBlockState(pos.down())) && world.getLightFor(EnumSkyBlock.SKY, pos) < 4) {
					world.setBlockState(pos, random.nextInt(3) == 0 ? ModBlocks.blockStates.redMushroom : ModBlocks.blockStates.brownMushroom);//Change branch to a mushroom
					world.setBlockState(pos.down(), ModBlocks.blockStates.podzol);//Change rooty dirt to Podzol
				}
				return true;
			}

			return false;
		}

		private boolean isGeneratingInWater(World world, BlockPos pos, IBlockState soilBlockState) {
			if (soilBlockState.getBlock() == Blocks.WATER) {
				Biome biome = world.getBiome(pos);
				if (BiomeDictionary.hasType(biome, Type.SWAMP)) {
					BlockPos down = pos.down();
					return isAcceptableSoil(world, down, world.getBlockState(down));
				}
			}
			return false;
		}

		@Override
		public boolean isAcceptableSoilForWorldgen(World world, BlockPos pos, IBlockState soilBlockState) {
			if (isGeneratingInWater(world, pos, soilBlockState)) {
				return true;
			}
			return super.isAcceptableSoilForWorldgen(world, pos, soilBlockState);
		}

		@Override
		public boolean generate(World world, BlockPos rootPos, Biome biome, Random random, int radius, SafeChunkBounds safeBounds) {
			if (isGeneratingInWater(world, rootPos, world.getBlockState(rootPos))) {
				if (radius >= 5) {
					return super.generate(world, rootPos.down(), biome, random, radius, safeBounds);
				}
				return false;
			}
			return super.generate(world, rootPos, biome, random, radius, safeBounds);
		}

	}

	/**
	 * This species drops no seeds at all.  One must craft the seed from an apple.
	 */
	public class SpeciesAppleOak extends Species {

		private static final String speciesName = "apple";

		public SpeciesAppleOak(TreeFamily treeFamily) {
			super(new ResourceLocation(treeFamily.getName().getResourceDomain(), speciesName), treeFamily);

			setRequiresTileEntity(true);

			//A bit stockier, smaller and slower than your basic oak
			setBasicGrowingParameters(0.4f, 10.0f, 1, 4, 0.7f);

			envFactor(Type.COLD, 0.75f);
			envFactor(Type.HOT, 0.75f);
			envFactor(Type.DRY, 0.25f);

			generateSeed();

			ModBlocks.blockApple.setSpecies(this);
			addGenFeature(new FeatureGenFruit(ModBlocks.blockApple).setRayDistance(4));
		}

		@Override
		public boolean isBiomePerfect(Biome biome) {
			return biome == Biomes.PLAINS;
		}

	}

	Species swampSpecies;
	Species appleSpecies;

	protected boolean isLocationSwampy(World world, BlockPos pos) {
		return BiomeDictionary.hasType(world.getBiome(pos), Type.SWAMP);
	}

	public TreeOak() {
		super(BlockPlanks.EnumType.OAK);
		hasConiferVariants = true;
		addConnectableVanillaLeaves((state) -> {
			return state.getBlock() instanceof BlockOldLeaf && (state.getValue(BlockOldLeaf.VARIANT) == BlockPlanks.EnumType.OAK);
		});

		// This will cause the swamp variation of the oak to grow when the player plants a common oak acorn in a swamp.
		addSpeciesLocationOverride((world, trunkPos) -> isLocationSwampy(world, trunkPos) ? swampSpecies : Species.NULLSPECIES);
	}

	@Override
	public void createSpecies() {
		setCommonSpecies(new SpeciesOak(this));
		swampSpecies = new SpeciesSwampOak(this);
		appleSpecies = new SpeciesAppleOak(this);
	}

	@Override
	public void registerSpecies(IForgeRegistry<Species> speciesRegistry) {
		super.registerSpecies(speciesRegistry);
		speciesRegistry.register(swampSpecies);
		speciesRegistry.register(appleSpecies);
	}

	@Override
	public List<Item> getRegisterableItems(List<Item> itemList) {
		itemList.add(appleSpecies.getSeed());//Since we generated the apple species internally we need to let the seed out to be registered.
		return super.getRegisterableItems(itemList);
	}

	@Override
	public boolean isThick() {
		return true;
	}

}
