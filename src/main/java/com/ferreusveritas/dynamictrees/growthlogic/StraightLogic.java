package com.ferreusveritas.dynamictrees.growthlogic;

import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.trees.Species;
import com.ferreusveritas.dynamictrees.util.CoordUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class StraightLogic extends NullLogic {

	private final int heightVariation;

	public StraightLogic(int heightVariation) {
		this.heightVariation = heightVariation;
	}

	@Override
	public int[] directionManipulation(World world, BlockPos pos, Species species, int radius, GrowSignal signal, int[] probMap) {
		return new int[] {0, 1, 0, 0, 0, 0};
	}

	@Override
	public float getEnergy(World world, BlockPos pos, Species species, float signalEnergy) {
		long day = world.getTotalWorldTime() / 24000L;
		int month = (int) day / 30;
		return signalEnergy * species.biomeSuitability(world, pos) + (CoordUtils.coordHashCode(pos.up(month), 2) % heightVariation);
	}

	@Override
	public EnumFacing newDirectionSelected(Species species, EnumFacing newDir, GrowSignal signal) {
		return EnumFacing.UP;
	}

}
