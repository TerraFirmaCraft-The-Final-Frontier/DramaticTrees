package com.ferreusveritas.dynamictrees.blocks;

import com.ferreusveritas.dynamictrees.ModBlocks;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.cells.CellNull;
import com.ferreusveritas.dynamictrees.api.cells.ICell;
import com.ferreusveritas.dynamictrees.api.treedata.ILeavesProperties;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.trees.Species;
import com.ferreusveritas.dynamictrees.trees.SpeciesMushroom;
import com.ferreusveritas.dynamictrees.trees.TreeFamilyMushroom;
import com.ferreusveritas.dynamictrees.util.BlockBounds;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockBranchMushroom extends BlockBranchThick {

	public BlockBranchMushroom(String name) {
		super(Material.WOOD, name, false);
		otherBlock = new BlockBranchMushroom(Material.WOOD, name + "x", true);
		otherBlock.otherBlock = this;
		cacheBranchThickStates();
		setSoundType(SoundType.WOOD);
		setFlammability(0);
		setFireSpreadSpeed(0);
	}

	protected BlockBranchMushroom(Material material, String name, boolean extended) {
		super(material, name, extended);
		setSoundType(SoundType.WOOD);
		setFlammability(0);
		setFireSpreadSpeed(0);
	}

	@Override
	public ICell getHydrationCell(IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing dir, ILeavesProperties leavesProperties) {
		return CellNull.NULLCELL;
	}

	@Override
	public GrowSignal growIntoAir(World world, BlockPos pos, GrowSignal signal, int fromRadius) {
		if (!(signal.getSpecies() instanceof SpeciesMushroom)) {
			return signal;
		}

		SpeciesMushroom species = (SpeciesMushroom) signal.getSpecies();
		BlockDynamicCapCenter cap = species.getCapProperties().getDynamicCapCenterBlock();
		if (cap != null) {
			if (fromRadius == getFamily().getPrimaryThickness()) {
				signal.success = cap.tryGrowCap(world, species.getCapProperties(), 0, signal, pos, pos, false);
			} else {
				return cap.branchOut(world, pos, signal, 0);
			}
		}
		return signal;
	}

	@Override
	protected void destroyLeaves(World world, BlockPos cutPos, Species species, List<BlockPos> endPoints, Map<BlockPos, IBlockState> destroyedLeaves, List<BlockItemStack> drops) {
		super.destroyLeaves(world, cutPos, species, endPoints, destroyedLeaves, drops);
		destroyMushroomCaps(world, cutPos, species, endPoints, drops);
	}

	protected void destroyMushroomCaps(World world, BlockPos cutPos, Species species, List<BlockPos> endPoints, List<BlockItemStack> drops) {
		if (world.isRemote || endPoints.isEmpty() || !(species instanceof SpeciesMushroom) || !(species.getFamily() instanceof TreeFamilyMushroom)) {
			return;
		}

		SpeciesMushroom mushroom = (SpeciesMushroom) species;
		TreeFamilyMushroom family = (TreeFamilyMushroom) species.getFamily();
		List<BlockPos> capPositions = new ArrayList<>();

		for (BlockPos endPos : endPoints) {
			BlockPos centerPos = endPos.up();
			int age = BlockDynamicCapCenter.getCapAge(world, centerPos);
			if (age >= 0) {
				capPositions.addAll(mushroom.getMushroomShape().getShapeCluster(world, centerPos, mushroom, age));
			}
		}

		BlockBounds bounds = capPositions.isEmpty() ? null : new BlockBounds(capPositions).expand(1);
		if (bounds == null) {
			return;
		}

		for (BlockPos pos : bounds.iterate()) {
			IBlockState state = world.getBlockState(pos);
			if (family.isCompatibleCap(mushroom, state, world, pos)) {
				CapProperties cap = getCapProperties(state);
				if (!cap.getMushroomItemStack(1).isEmpty() && world.rand.nextInt(10) == 0) {
					drops.add(new BlockItemStack(cap.getMushroomItemStack(1), pos.subtract(cutPos)));
				}
				world.setBlockState(pos, ModBlocks.blockStates.air, 0);
			}
		}
	}

	private CapProperties getCapProperties(IBlockState state) {
		if (state.getBlock() instanceof BlockDynamicCap) {
			return ((BlockDynamicCap) state.getBlock()).getProperties(state);
		}
		return CapProperties.NULL;
	}

	@Override
	public List<ItemStack> getLogDrops(World world, BlockPos pos, Species species, float volume) {
		if (species instanceof SpeciesMushroom) {
			return new ArrayList<>();
		}
		return super.getLogDrops(world, pos, species, volume);
	}

}
