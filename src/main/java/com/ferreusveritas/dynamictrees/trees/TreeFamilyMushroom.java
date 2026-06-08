package com.ferreusveritas.dynamictrees.trees;

import com.ferreusveritas.dynamictrees.ModConstants;
import com.ferreusveritas.dynamictrees.api.treedata.ILeavesProperties;
import com.ferreusveritas.dynamictrees.blocks.BlockBranch;
import com.ferreusveritas.dynamictrees.blocks.BlockBranchMushroom;
import com.ferreusveritas.dynamictrees.blocks.CapProperties;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.List;

public class TreeFamilyMushroom extends TreeFamily {

	public final CapProperties redCap;
	public final CapProperties brownCap;

	private SpeciesMushroom redMushroom;
	private SpeciesMushroom brownMushroom;
	private SpeciesMushroom megaRedMushroom;
	private SpeciesMushroom megaBrownMushroom;
	private boolean capPropertiesReady;

	public TreeFamilyMushroom() {
		super(new ResourceLocation(ModConstants.MODID, "mushroom"));
		setPrimitiveLog(Blocks.BROWN_MUSHROOM_BLOCK.getDefaultState(), new ItemStack(Blocks.BROWN_MUSHROOM_BLOCK));
		setStick(ItemStack.EMPTY);

		redCap = new CapProperties(new ResourceLocation(ModConstants.MODID, "red_mushroom"), Blocks.RED_MUSHROOM_BLOCK.getDefaultState(), Item.getItemFromBlock(Blocks.RED_MUSHROOM)).setFamily(this);
		brownCap = new CapProperties(new ResourceLocation(ModConstants.MODID, "brown_mushroom"), Blocks.BROWN_MUSHROOM_BLOCK.getDefaultState(), Item.getItemFromBlock(Blocks.BROWN_MUSHROOM)).setFamily(this);
		capPropertiesReady = true;
		createSpecies();
	}

	@Override
	public BlockBranch createBranch() {
		return new BlockBranchMushroom(getName() + "branch");
	}

	@Override
	public boolean isThick() {
		return true;
	}

	@Override
	public float getPrimaryThickness() {
		return 2.0f;
	}

	@Override
	public float getSecondaryThickness() {
		return 3.0f;
	}

	@Override
	public int getMaxSignalDepth() {
		return 64;
	}

	@Override
	public ILeavesProperties getCommonLeaves() {
		return com.ferreusveritas.dynamictrees.blocks.LeavesProperties.NULLPROPERTIES;
	}

	@Override
	public void createSpecies() {
		if (!capPropertiesReady) {
			return;
		}
		redMushroom = new SpeciesMushroom(new ResourceLocation(ModConstants.MODID, "mushroomred"), this, redCap, false, false);
		brownMushroom = new SpeciesMushroom(new ResourceLocation(ModConstants.MODID, "mushroombrn"), this, brownCap, false, true);
		megaRedMushroom = new SpeciesMushroom(new ResourceLocation(ModConstants.MODID, "megamushroomred"), this, redCap, true, false);
		megaBrownMushroom = new SpeciesMushroom(new ResourceLocation(ModConstants.MODID, "megamushroombrn"), this, brownCap, true, true);

		redMushroom.setMegaSpecies(megaRedMushroom);
		brownMushroom.setMegaSpecies(megaBrownMushroom);
		setCommonSpecies(redMushroom);
	}

	@Override
	public void registerSpecies(IForgeRegistry<Species> speciesRegistry) {
		redMushroom.generateSeed();
		brownMushroom.generateSeed();
		speciesRegistry.register(redMushroom);
		speciesRegistry.register(brownMushroom);
		speciesRegistry.register(megaRedMushroom);
		speciesRegistry.register(megaBrownMushroom);
	}

	@Override
	public List<Block> getRegisterableBlocks(List<Block> blockList) {
		super.getRegisterableBlocks(blockList);
		blockList.addAll(redCap.getRegisterableBlocks());
		blockList.addAll(brownCap.getRegisterableBlocks());
		return blockList;
	}

	@Override
	public List<Item> getRegisterableItems(List<Item> itemList) {
		super.getRegisterableItems(itemList);
		brownMushroom.getSeed().ifValid(s -> itemList.add(s));
		return itemList;
	}

	public SpeciesMushroom getRedMushroom() {
		return redMushroom;
	}

	public SpeciesMushroom getBrownMushroom() {
		return brownMushroom;
	}

	public boolean isCompatibleCap(SpeciesMushroom species, IBlockState state, World world, BlockPos pos) {
		return species.getCapProperties().isPartOfCap(state);
	}

}
