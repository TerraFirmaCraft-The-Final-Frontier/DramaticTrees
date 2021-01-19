package com.ferreusveritas.dynamictrees.blocks;

import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.blocks.branches.BranchBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;

public class CocoaFruitBlock extends CocoaBlock {
	
	public static final String name = "fruitcocoa";
	
	public CocoaFruitBlock() {
		this(name);
	}
	
	public CocoaFruitBlock(String name) {
		super(Block.Properties.create(Material.PLANTS)
				.tickRandomly()
				.hardnessAndResistance(0.2F, 3.0F)
				.sound(SoundType.WOOD));
		setRegistryName(name);
	}
	
	/**
	* Can this block stay at this position.  Similar to canPlaceBlockAt except gets checked often with plants.
	*/
	public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos) {
		BlockState logState = worldIn.getBlockState(pos.offset(state.get(HORIZONTAL_FACING)));
		BranchBlock branch = TreeHelper.getBranch(logState);
		return branch != null && branch.getRadius(logState) == 8 && branch.getFamily().canSupportCocoa;
	}

}
