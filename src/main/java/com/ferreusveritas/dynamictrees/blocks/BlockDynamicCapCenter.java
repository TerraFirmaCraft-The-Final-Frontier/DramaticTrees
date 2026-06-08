package com.ferreusveritas.dynamictrees.blocks;

import com.ferreusveritas.dynamictrees.ModBlocks;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.systems.mushroomlogic.MushroomCapDisc;
import com.ferreusveritas.dynamictrees.systems.mushroomlogic.Vec2i;
import com.ferreusveritas.dynamictrees.trees.SpeciesMushroom;
import com.ferreusveritas.dynamictrees.trees.TreeFamily;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockDynamicCapCenter extends BlockDynamicCap {

	public static final PropertyInteger AGE = PropertyInteger.create("age", 0, MushroomCapDisc.MAX_RADIUS);
	private static final AxisAlignedBB AGE_ZERO_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);

	public BlockDynamicCapCenter(CapProperties properties, String name) {
		super(properties, name, true);
		setDefaultState(blockState.getBaseState().withProperty(AGE, 0));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, AGE);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(AGE, meta & 7);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(AGE);
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, net.minecraft.entity.EntityLivingBase placer) {
		return getDefaultState();
	}

	@Override
	public GrowSignal growSignal(World world, BlockPos pos, GrowSignal signal) {
		if (!(signal.getSpecies() instanceof SpeciesMushroom)) {
			return signal;
		}

		SpeciesMushroom species = (SpeciesMushroom) signal.getSpecies();
		if (signal.step()) {
			IBlockState state = world.getBlockState(pos);
			int age = state.getBlock() == this ? state.getValue(AGE) : 0;
			if (age != 0 && world.rand.nextFloat() < species.getMushroomShape().getChanceToAge()) {
				tryGrowCap(world, properties, age, signal, pos, pos, true);
			} else {
				branchOut(world, pos, signal, age);
			}
		}

		return signal;
	}

	public GrowSignal branchOut(World world, BlockPos pos, GrowSignal signal, int age) {
		if (!(signal.getSpecies() instanceof SpeciesMushroom)) {
			return signal;
		}
		SpeciesMushroom species = (SpeciesMushroom) signal.getSpecies();

		if (isNextToBranch(world, pos, signal.dir.getOpposite())) {
			signal.success = false;
			return signal;
		}

		boolean couldGrow = tryGrowCap(world, species.getCapProperties(), age, signal, pos.offset(signal.dir), pos, false);

		if (couldGrow) {
			TreeFamily family = species.getFamily();
			IBlockState capCenter = world.getBlockState(pos.offset(signal.dir));
			int thickness = (int) family.getPrimaryThickness() + (capCenter.getBlock() instanceof BlockDynamicCapCenter ? capCenter.getValue(AGE) : 0);
			family.getDynamicBranch().setRadius(world, pos, Math.min(thickness, species.maxBranchRadius()), null);
			signal.radius = Math.min(thickness + 1, species.maxBranchRadius());
		}

		signal.success = couldGrow;
		return signal;
	}

	private boolean isNextToBranch(World world, BlockPos pos, EnumFacing except) {
		for (EnumFacing dir : EnumFacing.VALUES) {
			if (dir != except && TreeHelper.isBranch(world.getBlockState(pos.offset(dir)))) {
				return true;
			}
		}
		return false;
	}

	public boolean tryGrowCap(World world, CapProperties capProperties, int currentAge, GrowSignal signal, BlockPos pos, BlockPos previousPos, boolean forceAge) {
		if (!(signal.getSpecies() instanceof SpeciesMushroom)) {
			return false;
		}
		SpeciesMushroom species = (SpeciesMushroom) signal.getSpecies();

		if (world.isAirBlock(pos)) {
			int age = currentAge;
			if (currentAge == 0) {
				age = 1;
			} else if (forceAge || world.rand.nextFloat() < species.getMushroomShape().getChanceToAge()) {
				age = Math.min(age + 1, properties.getMaxAge(species));
			}

			world.setBlockState(pos, getCapBlockStateForPlacement(world, pos, age == 0 ? 1 : age, capProperties.getDynamicCapState(true), false), 2);
			if (age != currentAge) {
				ageBranchUnderCap(world, pos, signal, currentAge);
			}
			generateCap(age, world, species, pos, previousPos, currentAge);
			return true;
		}

		return TreeHelper.getTreePart(world.getBlockState(pos)) instanceof BlockDynamicCapCenter;
	}

	public IBlockState getCapBlockStateForPlacement(World world, BlockPos pos, int age, IBlockState cap, boolean worldGen) {
		return cap.getBlock() instanceof BlockDynamicCapCenter ? cap.withProperty(AGE, age) : cap;
	}

	protected void ageBranchUnderCap(World world, BlockPos pos, GrowSignal signal, int currentAge) {
		SpeciesMushroom species = (SpeciesMushroom) signal.getSpecies();
		TreeFamily family = species.getFamily();
		int thickness = Math.min((int) family.getPrimaryThickness() + currentAge, species.maxBranchRadius());

		BlockPos branchPos = pos.offset(signal.dir.getOpposite());
		family.getDynamicBranch().setRadius(world, branchPos, thickness, null);
		signal.radius = Math.min(thickness + 1, species.maxBranchRadius());
	}

	protected void generateCap(int newAge, World world, SpeciesMushroom species, BlockPos newPos, BlockPos currentPos, int currentAge) {
		if (!currentPos.equals(newPos) || currentAge != newAge) {
			species.getMushroomShape().clearMushroomCap(world, currentPos, species, currentAge);
		}
		species.getMushroomShape().generateMushroomCap(world, newPos, species, newAge);
	}

	public void clearRing(World world, BlockPos pos, int radius) {
		for (Vec2i vec : MushroomCapDisc.getPrecomputedRing(radius)) {
			BlockPos ringPos = pos.add(vec.x, 0, vec.z);
			if (properties.isPartOfCap(world.getBlockState(ringPos))) {
				world.setBlockState(ringPos, ModBlocks.blockStates.air, 2);
			}
		}
	}

	public boolean placeRing(World world, BlockPos pos, int radius, int step, boolean yMoved, boolean negativeFactor) {
		int placed = 0;
		int notPlaced = 0;

		for (Vec2i vec : MushroomCapDisc.getPrecomputedRing(radius)) {
			BlockPos ringPos = pos.add(vec.x, 0, vec.z);
			if (canCapReplace(world, ringPos, world.getBlockState(ringPos))) {
				world.setBlockState(ringPos, getStateForAge(properties, step, new Vec2i(-vec.x, -vec.z), yMoved, negativeFactor, properties.isPartOfCap(world.getBlockState(ringPos.up()))), 2);
				placed++;
			} else {
				notPlaced++;
			}
		}

		return placed >= notPlaced;
	}

	public List<BlockPos> getRing(World world, BlockPos pos, int radius) {
		List<BlockPos> positions = new ArrayList<>();
		for (Vec2i vec : MushroomCapDisc.getPrecomputedRing(radius)) {
			BlockPos ringPos = pos.add(vec.x, 0, vec.z);
			if (properties.isPartOfCap(world.getBlockState(ringPos))) {
				positions.add(ringPos);
			}
		}
		return positions;
	}

	public static boolean canCapReplace(World world, BlockPos pos, IBlockState state) {
		Block block = state.getBlock();
		return block == Blocks.AIR || block.isReplaceable(world, pos) || TreeHelper.isLeaves(state);
	}

	private IBlockState getStateForAge(CapProperties properties, int age, Vec2i centerDirection, boolean yMoved, boolean negativeFactor, boolean topIsCap) {
		boolean[] directions = {false, !topIsCap, true, true, true, true};
		if (yMoved || age == 1) {
			for (EnumFacing dir : EnumFacing.HORIZONTALS) {
				float dot = dir.getXOffset() * centerDirection.x + dir.getZOffset() * centerDirection.z;
				if (dot >= 0) {
					directions[negativeFactor ? dir.getOpposite().ordinal() : dir.ordinal()] = false;
				}
			}
		}
		return properties.getDynamicCapState(age, directions);
	}

	public static int getCapAge(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof BlockDynamicCapCenter) {
			return state.getValue(AGE);
		}
		return -1;
	}

	@Override
	public int branchSupport(IBlockState state, IBlockAccess world, BlockBranch branch, BlockPos pos, EnumFacing dir, int radius) {
		return branch.getFamily() == getFamily(state, world, pos) ? BlockBranch.setSupport(0, 2) : 0;
	}

	@Override
	public int getRadiusForConnection(IBlockState state, IBlockAccess world, BlockPos pos, BlockBranch from, EnumFacing side, int fromRadius) {
		return from.getFamily() == getFamily(state, world, pos) ? fromRadius : 0;
	}

	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return properties.getMushroomItemStack(1).getItem();
	}

	@Override
	public int quantityDropped(Random random) {
		return random.nextInt(10) == 0 ? 1 : 0;
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, net.minecraft.entity.player.EntityPlayer player) {
		return properties.getPrimitiveCapItemStack();
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return state.getBlock() == this && state.getValue(AGE) == 0 ? AGE_ZERO_AABB : super.getBoundingBox(state, source, pos);
	}

	@Override
	public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
		return true;
	}

	@Override
	public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		return true;
	}

}
