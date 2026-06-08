package com.ferreusveritas.dynamictrees.trees;

import com.ferreusveritas.dynamictrees.ModConstants;
import com.ferreusveritas.dynamictrees.ModSoundEvents;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.network.MapSignal;
import com.ferreusveritas.dynamictrees.blocks.BlockBranch;
import com.ferreusveritas.dynamictrees.blocks.BlockBranchThick;
import com.ferreusveritas.dynamictrees.blocks.BlockRooty;
import com.ferreusveritas.dynamictrees.blocks.CapProperties;
import com.ferreusveritas.dynamictrees.event.SpeciesPostGenerationEvent;
import com.ferreusveritas.dynamictrees.growthlogic.StraightLogic;
import com.ferreusveritas.dynamictrees.systems.DirtHelper;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.systems.mushroomlogic.MushroomShape;
import com.ferreusveritas.dynamictrees.systems.nodemappers.NodeFindEnds;
import com.ferreusveritas.dynamictrees.systems.nodemappers.NodeMushroomInflator;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import com.ferreusveritas.dynamictrees.worldgen.JoCode;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SpeciesMushroom extends Species {

	private final CapProperties capProperties;
	private final boolean mega;
	private final boolean brown;
	private final MushroomShape mushroomShape;
	private final int maxLightForPlanting = 12;

	public SpeciesMushroom(ResourceLocation name, TreeFamily treeFamily, CapProperties capProperties, boolean mega, boolean brown) {
		super(name, treeFamily, com.ferreusveritas.dynamictrees.blocks.LeavesProperties.NULLPROPERTIES);
		this.capProperties = capProperties.setFamily(treeFamily);
		this.mega = mega;
		this.brown = brown;
		this.mushroomShape = mega ? (brown ? MushroomShape.MEGA_BROWN : MushroomShape.MEGA_RED) : (brown ? MushroomShape.BROWN : MushroomShape.RED);

		setBasicGrowingParameters(0.0f, mega ? (brown ? 16.0f : 12.0f) : (brown ? 8.0f : 6.0f), 1, 0, 1.2f);
		setSoilLongevity(3);
		setGrowthLogicKit(new StraightLogic(5));
		clearAcceptableSoils();
		addAcceptableSoils(DirtHelper.FUNGUSLIKE);
		envFactor(Type.MUSHROOM, 1.15f);
	}

	public CapProperties getCapProperties() {
		return capProperties;
	}

	public MushroomShape getMushroomShape() {
		return mushroomShape;
	}

	@Override
	public boolean isMega() {
		return mega;
	}

	@Override
	public boolean isTransformable() {
		return !mega;
	}

	@Override
	public int maxBranchRadius() {
		return BlockBranchThick.RADMAX_THICK;
	}

	@Override
	public Species setupStandardSeedDropping() {
		return this;
	}

	@Override
	public boolean isAcceptableSoil(IBlockState soilBlockState) {
		if (soilBlockState.getBlock() == Blocks.WATER) {
			return false;
		}
		return DirtHelper.isSoilRegistered(soilBlockState.getBlock());
	}

	@Override
	public boolean isAcceptableSoil(World world, BlockPos pos, IBlockState soilBlockState) {
		if (!isAcceptableSoil(soilBlockState)) {
			return false;
		}
		return super.isAcceptableSoil(soilBlockState) || world.getLight(pos.up()) <= maxLightForPlanting;
	}

	@Override
	public boolean isAcceptableSoilForWorldgen(World world, BlockPos pos, IBlockState soilBlockState) {
		return isAcceptableSoil(soilBlockState);
	}

	@Override
	public boolean isBiomePerfect(Biome biome) {
		return isOneOfBiomes(biome, Biomes.MUSHROOM_ISLAND, Biomes.MUSHROOM_ISLAND_SHORE);
	}

	@Override
	public boolean transitionToTree(World world, BlockPos pos) {
		TreeFamily family = getFamily();
		if (world.isAirBlock(pos.up()) && isAcceptableSoil(world, pos.down(), world.getBlockState(pos.down()))) {
			family.getDynamicBranch().setRadius(world, pos, (int) family.getPrimaryThickness(), null);
			world.setBlockState(pos.up(), capProperties.getDynamicCapState(true, 0), 2);
			placeRootyDirtBlock(world, pos.down(), 15);
			return true;
		}
		return false;
	}

	@Override
	public AxisAlignedBB getSaplingBoundingBox() {
		return brown ? new AxisAlignedBB(0.1875D, 0.0D, 0.1875D, 0.8125D, 0.375D, 0.8125D) : new AxisAlignedBB(0.25D, 0.0D, 0.25D, 0.75D, 0.75D, 0.75D);
	}

	@Override
	public ResourceLocation getSaplingName() {
		return getRegistryName();
	}

	@Override
	public int saplingColorMultiplier(IBlockState state, net.minecraft.world.IBlockAccess access, BlockPos pos, int tintIndex) {
		return 0xFFFFFFFF;
	}

	@Override
	public SoundType getSaplingSound() {
		return SoundType.PLANT;
	}

	@Override
	public JoCode getJoCode(String joCodeString) {
		return new JoCodeMushroom(joCodeString);
	}

	@Override
	public void addJoCodes() {
		getJoCodeStore().addCodesFromFile(this,
			"assets/" + getRegistryName().getNamespace() + "/trees/" + getRegistryName().getPath() + ".txt");
	}

	@Override
	public boolean handleRot(World world, List<BlockPos> ends, BlockPos rootPos, BlockPos treePos, int soilLife, SafeChunkBounds safeBounds) {
		Iterator<BlockPos> iter = ends.iterator();
		while (iter.hasNext()) {
			BlockPos endPos = iter.next();
			IBlockState branchState = world.getBlockState(endPos);
			BlockBranch branch = TreeHelper.getBranch(branchState);
			if (branch != null) {
				int radius = branch.getRadius(branchState);
				if (branch.checkForRot(world, endPos, this, radius, world.rand, rotChance(world, endPos, world.rand, radius), safeBounds != SafeChunkBounds.ANY)) {
					iter.remove();
				}
			}
		}
		return ends.isEmpty() && !TreeHelper.isBranch(world.getBlockState(treePos));
	}

	@Override
	public boolean rot(World world, BlockPos pos, int neighborCount, int radius, Random random, boolean rapid) {
		if (rapid || radius <= com.ferreusveritas.dynamictrees.ModConfigs.maxBranchRotRadius) {
			BlockBranch branch = TreeHelper.getBranch(world.getBlockState(pos));
			if (branch != null) {
				branch.rot(world, pos);
				return true;
			}
		}
		return false;
	}

	@Override
	protected int[] customDirectionManipulation(World world, BlockPos pos, int radius, GrowSignal signal, int[] probMap) {
		return new int[] {0, 1, 0, 0, 0, 0};
	}

	@Override
	protected EnumFacing newDirectionSelected(EnumFacing newDir, GrowSignal signal) {
		return EnumFacing.UP;
	}

	@Override
	public SoundEvent getFallingTreeStartSound(float treeVolume, boolean hasLeaves) {
		return ModSoundEvents.FALLING_TREE_FUNGUS_START;
	}

	@Override
	public SoundEvent getFallingTreeEndSound(float treeVolume, boolean hasLeaves) {
		return ModSoundEvents.FALLING_TREE_FUNGUS_END;
	}

	@Override
	public SoundEvent getFallingBranchEndSound(float treeVolume, boolean hasLeaves, boolean fellOnWater) {
		return hasLeaves ? ModSoundEvents.FALLING_TREE_FUNGUS_SMALL_END : ModSoundEvents.FALLING_TREE_SMALL_END_BARE;
	}

	@Override
	public float getFallingTreePitch(float treeVolume) {
		return 2.0f / (1.0f + treeVolume * 0.1f);
	}

	private class JoCodeMushroom extends JoCode {

		public JoCodeMushroom(String code) {
			super(code);
		}

		@Override
		public void generate(World world, Species species, BlockPos rootPosIn, Biome biome, EnumFacing facing, int radius, SafeChunkBounds safeBounds) {
			boolean worldGen = safeBounds != SafeChunkBounds.ANY;
			radius = MathHelper.clamp(radius, 2, 8);

			setFacing(facing);
			BlockPos rootPos = species.preGeneration(world, rootPosIn, radius, facing, safeBounds, this);

			if (rootPos == BlockPos.ORIGIN) {
				return;
			}

			IBlockState initialDirtState = world.getBlockState(rootPos);
			species.placeRootyDirtBlock(world, rootPos, 0);

			generateFork(world, species, 0, rootPos, false);

			BlockPos treePos = rootPos.up();
			IBlockState treeState = world.getBlockState(treePos);
			BlockBranch branch = TreeHelper.getBranch(treeState);

			if (branch != null) {
				List<NodeMushroomInflator.CapAge> capAges = new ArrayList<>();
				NodeMushroomInflator inflator = new NodeMushroomInflator(SpeciesMushroom.this, capAges, radius, rootPos);
				NodeFindEnds endFinder = new NodeFindEnds();
				MapSignal signal = new MapSignal(inflator, endFinder);
				signal.destroyLoopedNodes = careful;
				branch.analyse(treeState, world, treePos, EnumFacing.DOWN, signal);

				if (signal.found || signal.overflow) {
					cleanupFrankentree(world, treePos, treeState, endFinder.getEnds(), safeBounds);
					return;
				}

				for (NodeMushroomInflator.CapAge capAge : capAges) {
					int age = Math.min(capAge.age, mushroomShape.getMaxCapAge());
					world.setBlockState(capAge.pos, capProperties.getDynamicCapState(true, age), worldGen ? 16 : 2);
					mushroomShape.generateMushroomCap(world, capAge.pos, SpeciesMushroom.this, age);
				}

				List<BlockPos> endPoints = endFinder.getEnds();
				if (species.handleRot(world, endPoints, rootPos, treePos, 0, safeBounds)) {
					return;
				}

				species.postGeneration(world, rootPos, biome, radius, endPoints, safeBounds, initialDirtState);
				MinecraftForge.EVENT_BUS.post(new SpeciesPostGenerationEvent(world, species, rootPos, endPoints, safeBounds, initialDirtState));
			} else {
				world.setBlockState(rootPos, initialDirtState, careful ? 3 : 2);
			}
		}

	}

}
