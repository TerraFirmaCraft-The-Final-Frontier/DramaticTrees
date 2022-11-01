package com.ferreusveritas.dynamictrees.event;

import com.ferreusveritas.dynamictrees.ModConfigs;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.WorldGenRegistry;
import com.ferreusveritas.dynamictrees.blocks.BlockBranch;
import com.ferreusveritas.dynamictrees.blocks.BlockTrunkShell;
import com.ferreusveritas.dynamictrees.client.TooltipHandler;
import com.ferreusveritas.dynamictrees.seasons.SeasonHelper;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.Type;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CommonEventHandler {

	@SubscribeEvent
	public void onWorldTick(WorldTickEvent event) {

		if (event.side == Side.SERVER) {
			FutureBreak.process(event.world);
		}

		if (event.type == Type.WORLD && event.phase == Phase.START) {
			SeasonHelper.updateTick(event.world, event.world.getWorldTime());
		}
	}

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		if (WorldGenRegistry.isWorldGenEnabled() && !event.getWorld().isRemote) {
			if (!WorldGenRegistry.validateBiomeDataBases()) {
				WorldGenRegistry.populateDataBase();
			}
		}

		event.getWorld().addEventListener(new WorldListener(event.getWorld(), event.getWorld().getMinecraftServer()));
	}

	@SubscribeEvent()
	public void onTreeClick(PlayerInteractEvent.LeftClickBlock event) {
		if (ModConfigs.treeStumping) {
			World world = event.getWorld();
			BlockPos blockPos = event.getPos();
			Block block = world.getBlockState(blockPos).getBlock();
			
			if (block instanceof BlockBranch || block instanceof BlockTrunkShell) {
				int blockPosY = event.getPos().getY();
				int blockPosStumpY = TreeHelper.findRootNode(world, blockPos).up().getY();
				
				if (blockPosY == blockPosStumpY) {
					event.setUseBlock(Event.Result.DENY);
					event.setUseItem(Event.Result.DENY);
					event.setCanceled(true);
				}
			}
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onItemTooltipAdded(ItemTooltipEvent event) {
		TooltipHandler.setupTooltips(event);
	}

}
