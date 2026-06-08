package com.ferreusveritas.dynamictrees.blocks;

import com.ferreusveritas.dynamictrees.api.cells.CellNull;
import com.ferreusveritas.dynamictrees.api.cells.ICell;
import com.ferreusveritas.dynamictrees.api.network.MapSignal;
import com.ferreusveritas.dynamictrees.api.treedata.ILeavesProperties;
import com.ferreusveritas.dynamictrees.api.treedata.ITreePart;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.systems.mushroomlogic.MushroomCapDisc;
import com.ferreusveritas.dynamictrees.trees.TreeFamily;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

public class BlockDynamicCap extends Block implements ITreePart {

	public static final PropertyInteger DISTANCE = PropertyInteger.create("distance", 1, MushroomCapDisc.MAX_RADIUS);
	public static final PropertyBool DOWN = PropertyBool.create("down");
	public static final PropertyBool UP = PropertyBool.create("up");
	public static final PropertyBool NORTH = PropertyBool.create("north");
	public static final PropertyBool SOUTH = PropertyBool.create("south");
	public static final PropertyBool WEST = PropertyBool.create("west");
	public static final PropertyBool EAST = PropertyBool.create("east");

	protected final CapProperties properties;

	public BlockDynamicCap(CapProperties properties, String name) {
		this(properties, name, false);
	}

	protected BlockDynamicCap(CapProperties properties, String name, boolean center) {
		super(properties.getMaterial());
		this.properties = properties;
		setRegistryName(name);
		setTranslationKey(name);
		setHardness(0.2F);
		setSoundType(properties.getSoundType());
		if (!center) {
			setDefaultState(blockState.getBaseState()
				.withProperty(DISTANCE, 1)
				.withProperty(DOWN, true)
				.withProperty(UP, true)
				.withProperty(NORTH, true)
				.withProperty(SOUTH, true)
				.withProperty(WEST, true)
				.withProperty(EAST, true));
		}
	}

	public CapProperties getProperties(IBlockState state) {
		return properties;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, DISTANCE, DOWN, UP, NORTH, SOUTH, WEST, EAST);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(DISTANCE, (meta & 7) + 1);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(DISTANCE) - 1;
	}

	public IProperty<?>[] getIgnorableProperties() {
		return new IProperty<?>[] {DISTANCE, DOWN, UP, NORTH, SOUTH, WEST, EAST};
	}

	public static IBlockState setDirectionValues(IBlockState state, boolean[] directions) {
		return state
			.withProperty(DOWN, directions[0])
			.withProperty(UP, directions[1])
			.withProperty(NORTH, directions[2])
			.withProperty(SOUTH, directions[3])
			.withProperty(WEST, directions[4])
			.withProperty(EAST, directions[5]);
	}

	public static boolean[] getDirectionValues(IBlockState state) {
		return new boolean[] {
			state.getValue(DOWN),
			state.getValue(UP),
			state.getValue(NORTH),
			state.getValue(SOUTH),
			state.getValue(WEST),
			state.getValue(EAST)
		};
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		return getDefaultState()
			.withProperty(DOWN, !properties.isPartOfCap(world.getBlockState(pos.down())))
			.withProperty(UP, !properties.isPartOfCap(world.getBlockState(pos.up())))
			.withProperty(NORTH, !properties.isPartOfCap(world.getBlockState(pos.north())))
			.withProperty(EAST, !properties.isPartOfCap(world.getBlockState(pos.east())))
			.withProperty(SOUTH, !properties.isPartOfCap(world.getBlockState(pos.south())))
			.withProperty(WEST, !properties.isPartOfCap(world.getBlockState(pos.west())));
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
		if (!world.isRemote) {
			world.scheduleUpdate(pos, this, 1);
		}
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
		if (world.getBlockState(pos).getBlock() != this) {
			return;
		}

		int distance = state.getValue(DISTANCE);
		boolean supportFound = false;
		for (BlockPos offPos : BlockPos.getAllInBoxMutable(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
			if (offPos.equals(pos)) {
				continue;
			}
			IBlockState offState = world.getBlockState(offPos);
			if ((offState.getBlock() instanceof BlockDynamicCap && offState.getValue(DISTANCE) == distance - 1)
				|| (distance == 1 && offState.getBlock() == properties.getDynamicCapCenterBlock())) {
				supportFound = true;
				break;
			}
		}

		if (!supportFound) {
			world.destroyBlock(pos, true);
		}
	}

	@Override
	public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
		if (rand.nextInt(50) == 0 && world.isAirBlock(pos.down())) {
			world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, pos.getX() + rand.nextDouble(), pos.getY() - 0.1D, pos.getZ() + rand.nextDouble(), 0.02D - rand.nextDouble() * 0.04D, -0.01D, 0.02D - rand.nextDouble() * 0.04D);
		}
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		boolean removed = super.removedByPlayer(state, world, pos, player, willHarvest);
		if (!world.isRemote && removed) {
			updateNeighborsSurround(world, pos);
		}
		return removed;
	}

	protected void updateNeighborsSurround(World world, BlockPos pos) {
		for (BlockPos offPos : BlockPos.getAllInBoxMutable(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
			if (!offPos.equals(pos) && world.getBlockState(offPos).getBlock() instanceof BlockDynamicCap) {
				world.scheduleUpdate(offPos.toImmutable(), world.getBlockState(offPos).getBlock(), 1);
			}
		}
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		return properties.getPrimitiveCapItemStack();
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
	public ICell getHydrationCell(IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing dir, ILeavesProperties leavesProperties) {
		return CellNull.NULLCELL;
	}

	@Override
	public GrowSignal growSignal(World world, BlockPos pos, GrowSignal signal) {
		return signal;
	}

	@Override
	public int probabilityForBlock(IBlockState state, IBlockAccess world, BlockPos pos, BlockBranch from) {
		return from.getFamily() == getFamily(state, world, pos) ? 2 : 0;
	}

	@Override
	public int getRadiusForConnection(IBlockState state, IBlockAccess world, BlockPos pos, BlockBranch from, EnumFacing side, int fromRadius) {
		return 0;
	}

	@Override
	public int getRadius(IBlockState state) {
		return 0;
	}

	@Override
	public boolean shouldAnalyse() {
		return false;
	}

	@Override
	public MapSignal analyse(IBlockState state, World world, BlockPos pos, @Nullable EnumFacing fromDir, MapSignal signal) {
		return signal;
	}

	@Override
	public TreeFamily getFamily(IBlockState state, IBlockAccess world, BlockPos pos) {
		return properties.getFamily();
	}

	@Override
	public int branchSupport(IBlockState state, IBlockAccess world, BlockBranch branch, BlockPos pos, EnumFacing dir, int radius) {
		return 0;
	}

	@Override
	public TreePartType getTreePartType() {
		return TreePartType.OTHER;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
		return state.getValue(getPropertyFor(face));
	}

	@Override
	public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		return state.getValue(getPropertyFor(side));
	}

	private PropertyBool getPropertyFor(EnumFacing face) {
		switch (face) {
			case DOWN:
				return DOWN;
			case UP:
				return UP;
			case NORTH:
				return NORTH;
			case SOUTH:
				return SOUTH;
			case WEST:
				return WEST;
			case EAST:
			default:
				return EAST;
		}
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.UNDEFINED;
	}

	@Override
	public EnumPushReaction getPushReaction(IBlockState state) {
		return EnumPushReaction.BLOCK;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.CUTOUT_MIPPED;
	}

	@Override
	public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
		super.onFallenUpon(world, pos, entity, fallDistance * 0.5F);
	}

	@Override
	public void onLanded(World world, Entity entity) {
		if (entity.isSneaking()) {
			super.onLanded(world, entity);
		} else {
			Vec3d vec = entity.motionY < 0.0D ? new Vec3d(entity.motionX, -entity.motionY * 0.66D, entity.motionZ) : new Vec3d(entity.motionX, entity.motionY, entity.motionZ);
			entity.motionX = vec.x;
			entity.motionY = vec.y;
			entity.motionZ = vec.z;
		}
	}

}
