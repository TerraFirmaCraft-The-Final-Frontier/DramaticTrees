package com.ferreusveritas.dynamictrees.systems.nodemappers;

import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.network.INodeInspector;
import com.ferreusveritas.dynamictrees.api.treedata.ITreePart;
import com.ferreusveritas.dynamictrees.blocks.BlockBranch;
import com.ferreusveritas.dynamictrees.blocks.BlockDynamicCapCenter;
import com.ferreusveritas.dynamictrees.systems.mushroomlogic.MushroomCapDisc;
import com.ferreusveritas.dynamictrees.trees.SpeciesMushroom;
import com.ferreusveritas.dynamictrees.util.CoordUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class NodeMushroomInflator implements INodeInspector {

	private static final int MIN_RADIUS_HEIGHT_DIVIDER = 3;

	private float radius;
	private BlockPos last;
	private BlockPos highestTrunkBlock;
	private final SpeciesMushroom species;
	private final List<CapAge> capAges;
	private final int generationRadius;
	private final BlockPos rootPos;
	private int lastCapBranchRadius;

	public NodeMushroomInflator(SpeciesMushroom species, List<CapAge> capAges, int generationRadius, BlockPos rootPos) {
		this.species = species;
		this.capAges = capAges;
		this.generationRadius = generationRadius;
		this.rootPos = rootPos;
		last = BlockPos.ORIGIN;
		highestTrunkBlock = null;
		lastCapBranchRadius = (int) species.getFamily().getPrimaryThickness();
	}

	@Override
	public boolean run(IBlockState blockState, World world, BlockPos pos, EnumFacing fromDir) {
		BlockBranch branch = TreeHelper.getBranch(blockState);

		if (branch != null) {
			radius = species.getFamily().getPrimaryThickness();
			if (highestTrunkBlock == null && !TreeHelper.isBranch(world.getBlockState(pos.up()))) {
				highestTrunkBlock = pos;
			}
		}

		return false;
	}

	@Override
	public boolean returnRun(IBlockState blockState, World world, BlockPos pos, EnumFacing fromDir) {
		BlockPos dist = pos.subtract(last);
		if (dist.getX() * dist.getX() + dist.getY() * dist.getY() + dist.getZ() * dist.getZ() != 1) {
			if (BlockDynamicCapCenter.canCapReplace(world, pos.up(), world.getBlockState(pos.up()))) {
				int height = pos.subtract(rootPos).getY();
				int maxAge = Math.min(Math.min(Math.min(species.getCapProperties().getMaxAge(species), MushroomCapDisc.MAX_RADIUS), height), generationRadius);
				int minAge = Math.max(0, height / MIN_RADIUS_HEIGHT_DIVIDER);
				int capAge = minAge + CoordUtils.coordHashCode(new BlockPos(pos.getX(), 0, pos.getZ()), 3) % (Math.abs(maxAge - minAge) + 1);
				lastCapBranchRadius = Math.min((int) species.getFamily().getPrimaryThickness() + capAge, species.maxBranchRadius());
				radius = lastCapBranchRadius;
				capAges.add(new CapAge(pos.up(), capAge));
			}
		}

		BlockBranch branch = TreeHelper.getBranch(blockState);

		if (branch != null) {
			float areaAccum = radius * radius;
			boolean isTwig = true;

			for (EnumFacing dir : EnumFacing.VALUES) {
				if (!dir.equals(fromDir)) {
					BlockPos dPos = pos.offset(dir);

					if (dPos.equals(last)) {
						isTwig = false;
						continue;
					}

					IBlockState deltaBlockState = world.getBlockState(dPos);
					ITreePart treepart = TreeHelper.getTreePart(deltaBlockState);
					if (branch.isSameTree(treepart)) {
						int branchRadius = treepart.getRadius(deltaBlockState);
						areaAccum += branchRadius * branchRadius;
					}
				}
			}

			if (isTwig) {
				branch.setRadius(world, pos, (int) radius, null);
			} else {
				radius = (float) Math.sqrt(areaAccum) + (species.getTapering() * species.getWorldGenTaperingFactor());

				int maxRadius = species.maxBranchRadius();
				if (radius > maxRadius) {
					radius = maxRadius;
				}

				if (highestTrunkBlock != null) {
					int blockRadius = 8;
					boolean isInTrunk = pos.getX() == highestTrunkBlock.getX() && pos.getY() <= highestTrunkBlock.getY() && pos.getZ() == highestTrunkBlock.getZ();
					if (radius > blockRadius && !isInTrunk) {
						radius = blockRadius;
					}
				}

				float secondaryThickness = Math.min(lastCapBranchRadius + 1, species.maxBranchRadius());
				if (radius < secondaryThickness) {
					radius = secondaryThickness;
				}

				branch.setRadius(world, pos, (int) Math.floor(radius), null);
			}

			last = pos;
		}

		return false;
	}

	public static class CapAge {
		public final BlockPos pos;
		public final int age;

		public CapAge(BlockPos pos, int age) {
			this.pos = pos;
			this.age = age;
		}
	}

}
