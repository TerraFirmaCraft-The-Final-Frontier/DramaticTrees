package com.ferreusveritas.dynamictrees.systems.mushroomlogic;

import com.ferreusveritas.dynamictrees.blocks.BlockDynamicCapCenter;
import com.ferreusveritas.dynamictrees.trees.SpeciesMushroom;
import com.ferreusveritas.dynamictrees.util.CoordUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MushroomShape {

	public static final MushroomShape RED = new MushroomShape(6, 0.75f, 3, 0.0f, 2.0f, 0.5f, 0.1f, 0);
	public static final MushroomShape BROWN = new MushroomShape(6, 0.75f, 3, 0.0f, 0.2f, 0.1f, 0.1f, 0);
	public static final MushroomShape MEGA_RED = new MushroomShape(8, 0.75f, 3, 0.0f, 2.0f, 0.4f, 0.1f, 0);
	public static final MushroomShape MEGA_BROWN = new MushroomShape(8, 0.75f, 3, 0.0f, 0.15f, 0.05f, 0.05f, 0);

	private final int maxCapAge;
	private final float chanceToAge;
	private final int curvePower;
	private final float curveHeightOffset;
	private final float minAgeCurveFactor;
	private final float maxAgeCurveFactor;
	private final float curveFactorVariation;
	private final int pointedTipAge;

	public MushroomShape(int maxCapAge, float chanceToAge, int curvePower, float curveHeightOffset, float minAgeCurveFactor, float maxAgeCurveFactor, float curveFactorVariation, int pointedTipAge) {
		this.maxCapAge = maxCapAge;
		this.chanceToAge = chanceToAge;
		this.curvePower = curvePower;
		this.curveHeightOffset = curveHeightOffset;
		this.minAgeCurveFactor = minAgeCurveFactor;
		this.maxAgeCurveFactor = maxAgeCurveFactor;
		this.curveFactorVariation = curveFactorVariation;
		this.pointedTipAge = pointedTipAge;
	}

	public int getMaxCapAge() {
		return maxCapAge;
	}

	public float getChanceToAge() {
		return chanceToAge;
	}

	public void generateMushroomCap(World world, BlockPos centerPos, SpeciesMushroom species, int age) {
		placeRing(world, centerPos, species, age, RingAction.PLACE);
	}

	public void clearMushroomCap(World world, BlockPos centerPos, SpeciesMushroom species, int age) {
		placeRing(world, centerPos, species, age, RingAction.CLEAR);
	}

	public List<BlockPos> getShapeCluster(World world, BlockPos centerPos, SpeciesMushroom species, int age) {
		return placeRing(world, centerPos, species, age, RingAction.GET);
	}

	private List<BlockPos> placeRing(World world, BlockPos centerPos, SpeciesMushroom species, int age, RingAction action) {
		BlockDynamicCapCenter centerBlock = species.getCapProperties().getDynamicCapCenterBlock();
		List<BlockPos> ringPositions = new ArrayList<>();
		if (centerBlock == null) {
			return ringPositions;
		}

		float factor = calculateFactor(centerPos, species, age);

		int y = 0;
		int radius = 1;
		for (int step = 1; step <= age; step++) {
			int nextY = factor == 0 ? 0 : (int) Math.floor(Math.pow(factor * radius, curvePower) - curveHeightOffset);

			boolean moveY = step == 1 ? age <= pointedTipAge : nextY != y;
			if (moveY) {
				y += (int) Math.signum(factor);
			}

			BlockPos ringPos = centerPos.down(y);
			if (action == RingAction.CLEAR) {
				centerBlock.clearRing(world, ringPos, radius);
			} else if (action == RingAction.PLACE) {
				if (!centerBlock.placeRing(world, ringPos, radius, step, moveY, factor < 0 && step < age)) {
					break;
				}
			} else if (action == RingAction.GET) {
				ringPositions.addAll(centerBlock.getRing(world, ringPos, radius));
			}

			if (step >= nextY) {
				radius++;
			}
		}

		ringPositions.add(centerPos);
		return ringPositions;
	}

	private float calculateFactor(BlockPos centerPos, SpeciesMushroom species, int age) {
		if (age == 0) {
			return 0;
		}

		float rand = (CoordUtils.coordHashCode(new BlockPos(centerPos.getX(), 0, centerPos.getZ()), 2) & 0xFFFF) / (float) 0xFFFF;
		float variation = (rand * curveFactorVariation * 2.0f) - curveFactorVariation;

		return ((float) age / species.getCapProperties().getMaxAge(species)) * (maxAgeCurveFactor - minAgeCurveFactor) + minAgeCurveFactor + variation;
	}

	private enum RingAction {
		PLACE,
		CLEAR,
		GET
	}

}
