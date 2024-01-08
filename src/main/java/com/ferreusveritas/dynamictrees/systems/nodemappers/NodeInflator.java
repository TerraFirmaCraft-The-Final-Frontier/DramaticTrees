package com.ferreusveritas.dynamictrees.systems.nodemappers;

import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.network.INodeInspector;
import com.ferreusveritas.dynamictrees.api.treedata.ITreePart;
import com.ferreusveritas.dynamictrees.blocks.BlockBranch;
import com.ferreusveritas.dynamictrees.trees.Species;
import com.ferreusveritas.dynamictrees.util.SimpleVoxmap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class NodeInflator implements INodeInspector {

	private float radius;
	private BlockPos last;
	private BlockPos highestTrunkBlock;

	Species species;
	SimpleVoxmap leafMap;

	public NodeInflator(Species species, SimpleVoxmap leafMap) {
		this.species = species;
		this.leafMap = leafMap;
		last = BlockPos.ORIGIN;
		highestTrunkBlock = null;
	}

	@Override
	public boolean run(IBlockState blockState, World world, BlockPos pos, EnumFacing fromDir) {
		BlockBranch branch = TreeHelper.getBranch(blockState);

		if (branch != null) {
			radius = species.getFamily().getPrimaryThickness();
			if (highestTrunkBlock == null && !TreeHelper.isBranch(world.getBlockState(pos.up())))
				highestTrunkBlock = pos;
		}

		return false;
	}

	@Override
	public boolean returnRun(IBlockState blockState, World world, BlockPos pos, EnumFacing fromDir) {
		//Calculate Branch Thickness based on neighboring branches

		BlockBranch branch = TreeHelper.getBranch(blockState);

		if (branch != null) {
			float areaAccum = radius * radius;//Start by accumulating the branch we just came from
			boolean isTwig = true;

			for (EnumFacing dir : EnumFacing.VALUES) {
				if (!dir.equals(fromDir)) {//Don't count where the signal originated from

					BlockPos dPos = pos.offset(dir);

					if (dPos.equals(last)) {//or the branch we just came back from
						isTwig = false;//on the return journey if the block we just came from is a branch we are obviously not the endpoint(twig)
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
				//Handle leaves here
				if (leafMap != null) {
					leafMap.setVoxel(pos, (byte) 16);//16(bit 5) is code for a twig
					SimpleVoxmap leafCluster = species.getLeavesProperties().getCellKit().getLeafCluster();
					leafMap.blitMax(pos, leafCluster);
				}
			} else {
				//The new branch should be the square root of all of the sums of the areas of the branches coming into it.
				radius = (float) Math.sqrt(areaAccum) + (species.getTapering() * species.getWorldGenTaperingFactor());

				//Ensure the branch is never inflated past it's species maximum
				int maxRadius = species.maxBranchRadius();
				if (radius > maxRadius) {
					radius = maxRadius;
				}

				if (highestTrunkBlock != null){
					//Ensure branches dont grow over 1 block thick if it isnt in the trunk
					int blockRadius = 8;
					boolean isInTrunk = (pos.getX() == highestTrunkBlock.getX() && pos.getY() <= highestTrunkBlock.getY() && pos.getZ() == highestTrunkBlock.getZ());
					if (radius > blockRadius && !isInTrunk){
						radius = blockRadius;
					}
				}

				//Ensure non-twig branches are at least radius 2
				float secondaryThickness = species.getFamily().getSecondaryThickness();
				if (radius < secondaryThickness) {
					radius = secondaryThickness;
				}

				branch.setRadius(world, pos, (int) Math.floor(radius), null);
				if (leafMap != null) {
					leafMap.setVoxel(pos, (byte) 32);//32(bit 6) is code for a branch
				}
			}

			last = pos;

		}

		return false;
	}

}
