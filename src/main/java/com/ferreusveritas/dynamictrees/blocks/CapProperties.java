package com.ferreusveritas.dynamictrees.blocks;

import com.ferreusveritas.dynamictrees.systems.mushroomlogic.MushroomCapDisc;
import com.ferreusveritas.dynamictrees.trees.SpeciesMushroom;
import com.ferreusveritas.dynamictrees.trees.TreeFamily;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.List;

public class CapProperties {

	public static final CapProperties NULL = new CapProperties(
		new ResourceLocation("dynamictrees", "null"),
		Blocks.AIR.getDefaultState(),
		Items.AIR
	) {
		@Override
		public BlockDynamicCap getDynamicCapBlock() {
			return null;
		}

		@Override
		public BlockDynamicCapCenter getDynamicCapCenterBlock() {
			return null;
		}

		@Override
		public IBlockState getDynamicCapState(boolean center) {
			return Blocks.AIR.getDefaultState();
		}

		@Override
		public IBlockState getDynamicCapState(boolean center, int prop) {
			return Blocks.AIR.getDefaultState();
		}

		@Override
		public IBlockState getDynamicCapState(int distance) {
			return Blocks.AIR.getDefaultState();
		}
	};

	private final ResourceLocation name;
	private final IBlockState primitiveCap;
	private final Item mushroomItem;
	private final BlockDynamicCap dynamicCapBlock;
	private final BlockDynamicCapCenter dynamicCapCenterBlock;
	private final IBlockState[] dynamicCapDistanceStates = new IBlockState[MushroomCapDisc.MAX_RADIUS + 1];
	private TreeFamily family = TreeFamily.NULLFAMILY;

	public CapProperties(ResourceLocation name, IBlockState primitiveCap, Item mushroomItem) {
		this.name = name;
		this.primitiveCap = primitiveCap;
		this.mushroomItem = mushroomItem;
		this.dynamicCapBlock = new BlockDynamicCap(this, name.getPath() + "_cap");
		this.dynamicCapCenterBlock = new BlockDynamicCapCenter(this, name.getPath() + "_cap_center");
	}

	public ResourceLocation getName() {
		return name;
	}

	public IBlockState getPrimitiveCap() {
		return primitiveCap;
	}

	public ItemStack getPrimitiveCapItemStack() {
		return new ItemStack(Item.getItemFromBlock(primitiveCap.getBlock()), 1, primitiveCap.getBlock().damageDropped(primitiveCap));
	}

	public ItemStack getMushroomItemStack(int count) {
		return mushroomItem == Items.AIR ? ItemStack.EMPTY : new ItemStack(mushroomItem, count);
	}

	public TreeFamily getFamily() {
		return family;
	}

	public CapProperties setFamily(TreeFamily family) {
		this.family = family;
		return this;
	}

	public BlockDynamicCap getDynamicCapBlock() {
		return dynamicCapBlock;
	}

	public BlockDynamicCapCenter getDynamicCapCenterBlock() {
		return dynamicCapCenterBlock;
	}

	public List<Block> getRegisterableBlocks() {
		if (dynamicCapBlock == null || dynamicCapCenterBlock == null) {
			return Collections.emptyList();
		}
		return java.util.Arrays.asList(dynamicCapBlock, dynamicCapCenterBlock);
	}

	public IBlockState getDynamicCapState(boolean center) {
		return getDynamicCapState(center, 1);
	}

	public IBlockState getDynamicCapState(boolean center, int prop) {
		if (center) {
			return dynamicCapCenterBlock.getDefaultState().withProperty(BlockDynamicCapCenter.AGE, Math.min(prop, MushroomCapDisc.MAX_RADIUS));
		}
		return getDynamicCapState(prop);
	}

	public IBlockState getDynamicCapState(int distance) {
		int clamped = Math.max(0, Math.min(MushroomCapDisc.MAX_RADIUS, distance));
		if (dynamicCapDistanceStates[clamped] == null) {
			dynamicCapDistanceStates[clamped] = clamped == 0 ? Blocks.AIR.getDefaultState() : dynamicCapBlock.getDefaultState().withProperty(BlockDynamicCap.DISTANCE, clamped);
		}
		return dynamicCapDistanceStates[clamped];
	}

	public IBlockState getDynamicCapState(int distance, boolean[] directions) {
		return BlockDynamicCap.setDirectionValues(getDynamicCapState(distance), directions);
	}

	public boolean isPartOfCap(IBlockState state) {
		Block block = state.getBlock();
		return block == dynamicCapBlock || block == dynamicCapCenterBlock;
	}

	public boolean isRed() {
		return primitiveCap.getBlock() == Blocks.RED_MUSHROOM_BLOCK;
	}

	public SoundType getSoundType() {
		return SoundType.WOOD;
	}

	public Material getMaterial() {
		return Material.WOOD;
	}

	public int getMaxAge(SpeciesMushroom species) {
		return species.getMushroomShape().getMaxCapAge();
	}

}
