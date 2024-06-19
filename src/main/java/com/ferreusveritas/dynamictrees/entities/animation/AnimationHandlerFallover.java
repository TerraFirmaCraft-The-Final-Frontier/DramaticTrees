package com.ferreusveritas.dynamictrees.entities.animation;

import com.ferreusveritas.dynamictrees.ModConfigs;
import com.ferreusveritas.dynamictrees.ModSoundEvents;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.network.MapSignal;
import com.ferreusveritas.dynamictrees.blocks.BlockBranch;
import com.ferreusveritas.dynamictrees.entities.EntityFallingTree;
import com.ferreusveritas.dynamictrees.systems.nodemappers.NodeExtState;
import com.ferreusveritas.dynamictrees.trees.TreeCactus;
import com.ferreusveritas.dynamictrees.util.BranchDestructionData;
import com.google.common.base.Predicates;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class AnimationHandlerFallover implements IAnimationHandler {

	@Override
	public String getName() {
		return "fallover";
	}

	class HandlerData extends AnimationHandlerData {

		float fallSpeed = 0;
		int bounces = 0;
		HashSet<EntityLivingBase> entitiesHit = new HashSet<>();//A record of the entities that have taken damage to ensure they are only damaged a single time

	}

	HandlerData getData(EntityFallingTree entity) {
		return entity.animationHandlerData instanceof HandlerData ? (HandlerData) entity.animationHandlerData : new HandlerData();
	}

	@Override
	public void initMotion(EntityFallingTree entity) {
		entity.animationHandlerData = new HandlerData();
		EntityFallingTree.standardDropLeavesPayLoad(entity);//Seeds and stuff fall out of the tree before it falls over

		BlockPos belowBlock = entity.getDestroyData().cutPos.down();
		if (entity.world.getBlockState(belowBlock).isSideSolid(entity.world, belowBlock, EnumFacing.UP)) {
			entity.onGround = true;
		}
		
		if (!(entity.getDestroyData().species.getFamily() instanceof TreeCactus)) {//Exempt cacti
			if (entity.getDestroyData().trunkHeight < 8) {//Play sound according to tree size
				entity.world.playSound(null, entity.getPosition(), ModSoundEvents.TREE_CRACK_SMALL, SoundCategory.AMBIENT, 0.8F, 1.0F);
			} else {
				entity.world.playSound(null, entity.getPosition(), ModSoundEvents.TREE_CRACK_LARGE, SoundCategory.AMBIENT, 0.8F, 1.0F);
			}
		}
	}

	@Override
	public void handleMotion(EntityFallingTree entity) {

		float fallSpeed = getData(entity).fallSpeed;

		if (entity.onGround) {
			float height = (float) entity.getMassCenter().y * 2;
			fallSpeed += (0.2 / height);
			addRotation(entity, fallSpeed);
		}

		entity.motionY -= AnimationConstants.TREE_GRAVITY;
		entity.posY += entity.motionY;

		{//Handle entire entity falling and collisions with its base and the ground
			World world = entity.world;
			int radius = 8;
			IBlockState state = entity.getDestroyData().getBranchBlockState(0);
			if (TreeHelper.isBranch(state)) {
				radius = ((BlockBranch) state.getBlock()).getRadius(state);
			}
			AxisAlignedBB fallBox = new AxisAlignedBB(entity.posX - radius, entity.posY, entity.posZ - radius, entity.posX + radius, entity.posY + 1.0, entity.posZ + radius);
			BlockPos pos = new BlockPos(entity.posX, entity.posY, entity.posZ);
			IBlockState collState = world.getBlockState(pos);
			AxisAlignedBB collBox = collState.getCollisionBoundingBox(world, pos);

			if (collBox != null) {
				collBox = collBox.offset(pos);
				if (fallBox.intersects(collBox)) {
					entity.motionY = 0;
					entity.posY = collBox.maxY;
					entity.prevPosY = entity.posY;
					entity.onGround = true;
				}
			}
		}

		if (fallSpeed > 0 && testCollision(entity)) {
			float fallSpeedPrev = fallSpeed;
			addRotation(entity, -fallSpeed);//pull back to before the collision
			getData(entity).bounces++;
			fallSpeed *= -AnimationConstants.TREE_ELASTICITY;//bounce with elasticity
			entity.landed = Math.abs(fallSpeed) < 0.02f;//The entity has landed if after a bounce it has little velocity
			if(fallSpeedPrev > 0.1F && !entity.world.isRemote) entity.world.playSound(null, entity.getPosition(), ModSoundEvents.TREE_LANDING, SoundCategory.AMBIENT, (Math.min(1.5F, fallSpeedPrev) / 1.5F) * 0.6F + 0.1F, entity.world.rand.nextFloat() * 0.2F + 0.6F);
		}

		//Crush living things with clumsy dead trees
		World world = entity.world;
		if (ModConfigs.enableFallingTreeDamage && !world.isRemote) {
			List<EntityLivingBase> elist = testEntityCollision(entity);
			for (EntityLivingBase living : elist) {
				if (!getData(entity).entitiesHit.contains(living)) {
					getData(entity).entitiesHit.add(living);
					float damage = entity.getDestroyData().woodVolume * Math.abs(fallSpeed) * 3f;
					if (getData(entity).bounces == 0 && damage > 2) {
						//System.out.println("damage: " + damage);
						living.motionX += world.rand.nextFloat() * entity.getDestroyData().toolDir.getOpposite().getFrontOffsetX() * damage * 0.2f;
						living.motionX += world.rand.nextFloat() - 0.5;
						living.motionY += world.rand.nextFloat() * fallSpeed * 0.25f;
						living.motionZ += world.rand.nextFloat() * entity.getDestroyData().toolDir.getOpposite().getFrontOffsetZ() * damage * 0.2f;
						living.motionZ += world.rand.nextFloat() - 0.5;
						damage *= ModConfigs.fallingTreeDamageMultiplier;
						//System.out.println("Tree Falling Damage: " + damage + "/" + living.getHealth());
						living.attackEntityFrom(AnimationConstants.TREE_DAMAGE, damage);
					}
				}
			}
		}

		getData(entity).fallSpeed = fallSpeed;
	}

	/**
	 * This tests a bounding box cube for each block of the trunk. Processing is approximately equivalent to the same
	 * number of {@link EntityItem}s in the world.
	 *
	 * @param entity
	 * @return true if collision is detected
	 */
	private boolean testCollision(EntityFallingTree entity) {
		EnumFacing toolDir = entity.getDestroyData().toolDir;

		float actingAngle = toolDir.getAxis() == EnumFacing.Axis.X ? entity.rotationYaw : entity.rotationPitch;

		int offsetX = toolDir.getFrontOffsetX();
		int offsetZ = toolDir.getFrontOffsetZ();
		float h = MathHelper.sin((float) Math.toRadians(actingAngle)) * (offsetX | offsetZ);
		float v = MathHelper.cos((float) Math.toRadians(actingAngle));
		float xbase = (float) (entity.posX + offsetX * (-(0.5f) + (v * 0.5f) + (h * 0.5f)));
		float ybase = (float) (entity.posY - (h * 0.5f) + (v * 0.5f));
		float zbase = (float) (entity.posZ + offsetZ * (-(0.5f) + (v * 0.5f) + (h * 0.5f)));

		int trunkHeight = entity.getDestroyData().trunkHeight;
		float maxRadius = entity.getDestroyData().getBranchRadius(0) / 16.0f;

		trunkHeight = Math.min(trunkHeight, 24);

		for (int segment = 0; segment < trunkHeight; segment++) {
			float segX = xbase + h * segment * offsetX;
			float segY = ybase + v * segment;
			float segZ = zbase + h * segment * offsetZ;
			float tex = 0.0625f;
			float half = MathHelper.clamp(tex * (segment + 1) * 2, tex, maxRadius);
			AxisAlignedBB testBB = new AxisAlignedBB(segX - half, segY - half, segZ - half, segX + half, segY + half, segZ + half);
			
			if (!entity.world.getCollisionBoxes(entity, testBB).isEmpty()) {
				if (ModConfigs.enableFallingTreeDomino) {
					int solidBlock = 0;
					Iterable<BlockPos> blocks = BlockPos.getAllInBox(
						(int) Math.floor(testBB.minX) - 1,
						(int) Math.floor(testBB.minY) - 1,
						(int) Math.floor(testBB.minZ) - 1,
						(int) Math.ceil(testBB.maxX) + 1,
						(int) Math.ceil(testBB.maxY) + 1,
						(int) Math.ceil(testBB.maxZ) + 1);
					
					for (BlockPos collBlockPos : blocks) {
						IBlockState collBlockState = entity.world.getBlockState(collBlockPos);
						Block collBlock = collBlockState.getBlock();
						if (TreeHelper.isBranch(collBlock)) {// Check for branch
							BlockPos dominoPos = ModConfigs.treeStumping ? TreeHelper.findRootNode(entity.world, collBlockPos).up(2) : TreeHelper.findRootNode(entity.world, collBlockPos).up();
							NodeExtState extStateMapper = new NodeExtState(dominoPos);
							((BlockBranch) collBlock).analyse(collBlockState, entity.world, dominoPos, null, new MapSignal(extStateMapper));
							//Calculate other trunk height
							int dominoTrunkHeight = 1;
							for (BlockPos iter = new BlockPos(0, 1, 0); extStateMapper.getExtStateMap().containsKey(iter); iter = iter.up()) {
								dominoTrunkHeight++;
							}
							if (entity.getDestroyData().trunkHeight > dominoTrunkHeight) {//Compare trunk heights
								//Break other tree
								((BlockBranch) collBlock).dominoBreak(entity.world, dominoPos, toolDir);
								solidBlock++;
							}
						} else if (TreeHelper.isLeaves(collBlock) || collBlock instanceof BlockLeaves || collBlock instanceof BlockTallGrass) {//Check for foliage
							//Break foliage
							entity.world.destroyBlock(collBlockPos, false);
						} else {
							solidBlock++;
						}
					}
					return solidBlock > 0;
				}
				else {
					return true;
				}
			}
		}
		return false;
	}

	private void addRotation(EntityFallingTree entity, float delta) {
		EnumFacing toolDir = entity.getDestroyData().toolDir;

		switch (toolDir) {
			case NORTH:
				entity.rotationPitch += delta;
				break;
			case SOUTH:
				entity.rotationPitch -= delta;
				break;
			case WEST:
				entity.rotationYaw += delta;
				break;
			case EAST:
				entity.rotationYaw -= delta;
				break;
			default:
				break;
		}

		entity.rotationPitch = MathHelper.wrapDegrees(entity.rotationPitch);
		entity.rotationYaw = MathHelper.wrapDegrees(entity.rotationYaw);
	}

	public List<EntityLivingBase> testEntityCollision(EntityFallingTree entity) {

		World world = entity.world;

		EnumFacing toolDir = entity.getDestroyData().toolDir;

		float actingAngle = toolDir.getAxis() == EnumFacing.Axis.X ? entity.rotationYaw : entity.rotationPitch;

		int offsetX = toolDir.getFrontOffsetX();
		int offsetZ = toolDir.getFrontOffsetZ();
		float h = MathHelper.sin((float) Math.toRadians(actingAngle)) * (offsetX | offsetZ);
		float v = MathHelper.cos((float) Math.toRadians(actingAngle));
		float xbase = (float) (entity.posX + offsetX * (-(0.5f) + (v * 0.5f) + (h * 0.5f)));
		float ybase = (float) (entity.posY - (h * 0.5f) + (v * 0.5f));
		float zbase = (float) (entity.posZ + offsetZ * (-(0.5f) + (v * 0.5f) + (h * 0.5f)));
		int trunkHeight = entity.getDestroyData().trunkHeight;
		float segX = xbase + h * (trunkHeight - 1) * offsetX;
		float segY = ybase + v * (trunkHeight - 1);
		float segZ = zbase + h * (trunkHeight - 1) * offsetZ;

		float maxRadius = entity.getDestroyData().getBranchRadius(0) / 16.0f;

		Vec3d vec3d1 = new Vec3d(xbase, ybase, zbase);
		Vec3d vec3d2 = new Vec3d(segX, segY, segZ);

		return world.getEntitiesInAABBexcluding(entity, new AxisAlignedBB(vec3d1.x, vec3d1.y, vec3d1.z, vec3d2.x, vec3d2.y, vec3d2.z),
			Predicates.and(
				EntitySelectors.NOT_SPECTATING,
				apply -> {
					if (apply instanceof EntityLivingBase && apply.canBeCollidedWith()) {
						AxisAlignedBB axisalignedbb = apply.getEntityBoundingBox().grow(maxRadius);
						return axisalignedbb.contains(vec3d1) || axisalignedbb.calculateIntercept(vec3d1, vec3d2) != null;
					}

					return false;
				}
			)
		).stream().map(a -> (EntityLivingBase) a).collect(Collectors.toList());

	}

	@Override
	public void dropPayload(EntityFallingTree entity) {
		World world = entity.world;
		BlockPos cutPos = entity.getDestroyData().cutPos;
		entity.getPayload().forEach(i -> Block.spawnAsEntity(world, cutPos, i));
	}

	@Override
	public boolean shouldDie(EntityFallingTree entity) {
		
		//boolean dead = entity.landed && entity.attackEntityFrom(DamageSource.GENERIC, 4);

		boolean dead =
			Math.abs(entity.rotationPitch) >= 160 ||
				Math.abs(entity.rotationYaw) >= 160 ||
				entity.landed ||
				entity.ticksExisted > 300 + (entity.getDestroyData().trunkHeight);

		//Force the Rooty Dirt to update if it's there.  Turning it back to dirt.
		if (dead) {
			entity.cleanupRootyDirt();
		}

		return dead;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void renderTransform(EntityFallingTree entity, float entityYaw, float partialTicks) {

		float yaw = MathHelper.wrapDegrees(com.ferreusveritas.dynamictrees.util.MathHelper.angleDegreesInterpolate(entity.prevRotationYaw, entity.rotationYaw, partialTicks));
		float pit = MathHelper.wrapDegrees(com.ferreusveritas.dynamictrees.util.MathHelper.angleDegreesInterpolate(entity.prevRotationPitch, entity.rotationPitch, partialTicks));

		//Vec3d mc = entity.getMassCenter();

		int radius = entity.getDestroyData().getBranchRadius(0);

		EnumFacing toolDir = entity.getDestroyData().toolDir;
		Vec3d toolVec = new Vec3d(toolDir.getFrontOffsetX(), toolDir.getFrontOffsetY(), toolDir.getFrontOffsetZ()).scale(radius / 16.0f);

		GlStateManager.translate(-toolVec.x, -toolVec.y, -toolVec.z);
		GlStateManager.rotate(-yaw, 0, 0, 1);
		GlStateManager.rotate(pit, 1, 0, 0);
		GlStateManager.translate(toolVec.x, toolVec.y, toolVec.z);

		GlStateManager.translate(-0.5, 0, -0.5);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldRender(EntityFallingTree entity) {
		return true;
	}

}
