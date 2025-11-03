/*
 * @file RedstonePenItem.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 */
package wfphantom.redstonequill.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import wfphantom.redstonequill.ModContent;
import wfphantom.redstonequill.blocks.RedstoneTrack;
import wfphantom.redstonequill.libmc.Inventories;
import wfphantom.redstonequill.libmc.StandardItems;

import java.util.Objects;

public class RedstoneQuillItem extends StandardItems.BaseItem {
    public RedstoneQuillItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return (state.getBlock().defaultDestroyTime() < 0.5f) ? 10000f : 0f;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player player) {
        // Hand needs to be guessed here.
        ItemStack stack = player.getItemInHand(player.getUsedItemHand());
        if (!isPen(stack)) stack = player.getMainHandItem();
        if (!isPen(stack)) stack = player.getOffhandItem();
        if (isPen(stack)) attack(stack, pos, player);
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        final Player player = context.getPlayer();
        final InteractionHand hand = context.getHand();
        final BlockPos pos = context.getClickedPos();
        final Direction facing = context.getClickedFace();
        final Level world = context.getLevel();
        final BlockState state = world.getBlockState(pos);
        final ItemStack stack = context.getItemInHand();
        // Add to track
        if (state.getBlock() instanceof RedstoneTrack.RedstoneTrackBlock track) {
            if (world.isClientSide()) return InteractionResult.SUCCESS;
            final BlockHitResult rtr = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
            return track.modifySegments(state, world, pos, player, stack, hand, rtr, false, true);
        }
        // Check if a new track can be placed.
        if (!RedstoneTrack.RedstoneTrackBlock.canBePlacedOnFace(state, world, pos, facing)) {
            // Cannot place here.
            return InteractionResult.FAIL;
        }
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        // Place new track
        final BlockPos target_pos = pos.relative(facing);
        final BlockState target_state = world.getBlockState(target_pos);
        if (target_state.getBlock() instanceof RedstoneTrack.RedstoneTrackBlock track_block) {
            // Add/remove tracks to existing RedstoneTrackBlock
            final BlockHitResult rtr = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), target_pos, context.isInside());
            return track_block.modifySegments(target_state, world, target_pos, player, stack, hand, rtr, false, true);
        } else {
            final BlockHitResult rtr = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), target_pos, context.isInside());
            final BlockPlaceContext ctx = new BlockPlaceContext(Objects.requireNonNull(player), context.getHand(), new ItemStack(Items.REDSTONE), rtr);
            final BlockState rs_state = ModContent.references.TRACK_BLOCK.getStateForPlacement(ctx);
            if (rs_state == null) return InteractionResult.FAIL;
            if (!target_state.canBeReplaced(ctx)) return InteractionResult.FAIL;
            if (!world.setBlock(target_pos, rs_state, 1 | 2 | 16)) return InteractionResult.FAIL;
            final BlockState placed_state = world.getBlockState(target_pos);
            if (placed_state.getBlock() instanceof RedstoneTrack.RedstoneTrackBlock track_block) {
                return (track_block.modifySegments(target_state, world, target_pos, player, stack, hand, rtr, false, true) == InteractionResult.FAIL) ? InteractionResult.FAIL : InteractionResult.CONSUME;
            } else {
                world.removeBlock(target_pos, false);
                return InteractionResult.FAIL;
            }
        }
    }

    private void attack(ItemStack stack, BlockPos pos, Player player) {
        final Level world = player.getCommandSenderWorld();
        final BlockState state = world.getBlockState(pos);
        if (state.is(ModContent.references.TRACK_BLOCK)) {
            final HitResult rt = player.pick(10.0, 0f, false);
            if (rt.getType() != HitResult.Type.BLOCK) return;
            final InteractionHand hand = (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == this) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (!(state.getBlock() instanceof RedstoneTrack.RedstoneTrackBlock track)) return;
            track.modifySegments(state, player.getCommandSenderWorld(), pos, player, stack, hand, ((BlockHitResult) rt), true, false);
        } else if (state.is(Blocks.REDSTONE_WIRE)) {
            pushRedstone(stack, 1, player);
            world.removeBlock(pos, false);
        }
    }

    public static void pushRedstone(ItemStack stack, int amount, Player player) {
        if (player.isCreative()) return;
        if (amount > 0) {
            if (isPen(stack)) {
                if (stack.getMaxDamage() <= 0) {
                    ItemStack remaining = Inventories.insert(player, new ItemStack(Items.REDSTONE, amount), false);
                    if (!remaining.isEmpty()) Inventories.give(player, remaining); // also drops, but with sound.
                } else if (stack.getDamageValue() >= amount) stack.setDamageValue(stack.getDamageValue() - amount);
                else {
                    amount -= stack.getDamageValue();
                    stack.setDamageValue(0);
                    Inventories.give(player, new ItemStack(Items.REDSTONE, amount));
                }
            } else if (stack.getItem() == Items.REDSTONE) {
                if (stack.getCount() <= stack.getMaxStackSize() - amount) stack.grow(amount);
                else Inventories.give(player, new ItemStack(Items.REDSTONE, amount));
            } else Inventories.give(player, new ItemStack(Items.REDSTONE, amount));
        }
    }

    public static void popRedstone(ItemStack stack, int amount, Player player, InteractionHand hand) {
        if (player.isCreative()) return;
        if (amount <= 0) return;
        Inventories.extract(player, new ItemStack(Items.REDSTONE), amount, false).getCount();
        if (stack.getItem() == Items.REDSTONE) {
            if (stack.getCount() <= amount) player.setItemInHand(hand, ItemStack.EMPTY);
            else stack.shrink(amount);
        }
    }

    public static boolean hasEnoughRedstone(ItemStack stack, int amount, Player player) {
        if (player.isCreative()) return true;
        if (isPen(stack)) {
            if (stack.getMaxDamage() > 0) return stack.getDamageValue() < (stack.getMaxDamage() - amount);
            else return Inventories.extract(player, new ItemStack(Items.REDSTONE), amount, true).getCount() >= amount;
        } else if (stack.getItem() == Items.REDSTONE) return (stack.getCount() >= amount);
        else return false;
    }

    public static boolean isPen(ItemStack stack) {
        return (stack.getItem() instanceof RedstoneQuillItem);
    }
}
