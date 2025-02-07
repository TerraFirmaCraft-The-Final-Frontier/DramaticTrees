package com.ferreusveritas.dynamictrees.entities;

import com.ferreusveritas.dynamictrees.api.substances.ISubstanceEffect;
import com.ferreusveritas.dynamictrees.blocks.BlockRooty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EntityLingeringEffector extends Entity {

	public BlockPos blockPos;
	public ISubstanceEffect effect;
	public boolean extended;

	public EntityLingeringEffector(World world, BlockPos pos, ISubstanceEffect effect) {
		super(world);
		width = 1.0f;
		height = 1.0f;
		noClip = true;
		setBlockPos(pos);
		setEffect(effect);

		if (this.effect != null) {
			//Search for existing effectors with the same effect in the same place
			for (EntityLingeringEffector effector : world.getEntitiesWithinAABB(EntityLingeringEffector.class, new AxisAlignedBB(pos))) {
				if (effector.getBlockPos().equals(pos) && effector.getEffect().getName().equals(effect.getName())) {
					effector.setDead();//Kill old effector if it's the same
				}
			}
		}
	}

	public static boolean treeHasEffectorForEffect(World world, BlockPos pos, ISubstanceEffect effect) {
		for (final EntityLingeringEffector effector : world.getEntitiesWithinAABB(EntityLingeringEffector.class, new AxisAlignedBB(pos))) {
			if (effector.getEffect() != null && effector.getEffect().getName().equals(effect.getName())) {
				return true;
			}
		}
		return false;
	}

	public void setBlockPos(BlockPos pos) {
		blockPos = pos;
		setPosition(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
	}

	public BlockPos getBlockPos() {
		return blockPos;
	}

	public void setEffect(ISubstanceEffect effect) {
		this.effect = effect;
	}

	public ISubstanceEffect getEffect() {
		return this.effect;
	}

	@Override
	protected void entityInit() {
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
	}

	private byte invalidTicks = 0;

	@Override
	public void onUpdate() {
		super.onUpdate();

		if (this.effect == null) {
			// If effect hasn't been set for 20 ticks then kill the entity.
			if (++this.invalidTicks > 20) {
				this.setDead();
			}
			return;
		}

		if (effect != null) {
			IBlockState state = world.getBlockState(blockPos);

			if (state.getBlock() instanceof BlockRooty) {
				if (!effect.update(world, blockPos, ticksExisted, state.getValue(BlockRooty.LIFE))) {
					setDead();
				}
			} else {
				setDead();
			}
		}

	}

	@Override
	public boolean shouldRenderInPass(int pass) {
		return false;//Effectively make this entity invisible
	}

}
