/*
 * @file StandardBlocks.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Common functionality class for decor blocks.
 */
package wfphantom.redstonequill.libmc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StandardBlocks {
    public static final long CFG_DEFAULT     = 0x0000000000000000L; // no special config
    public static final long CFG_AI_PASSABLE = 0x0000000000000800L; // does not block movement path for AI, needed for non-opaque blocks with collision shapes not thin at the bottom or one side.

    public interface IStandardBlock {
        default boolean hasDynamicDropList() {
            return false;
        }

        default List<ItemStack> dropList(BlockState state, Level world, @Nullable BlockEntity te, boolean explosion) {
            return Collections.singletonList((!world.isClientSide()) ? (new ItemStack(state.getBlock().asItem())) : (ItemStack.EMPTY));
        }
    }

    public static class BaseBlock extends Block implements IStandardBlock {
        public final long config;

        public BaseBlock(long conf, BlockBehaviour.Properties properties) {
            super(properties);
            config = conf;
            BlockState state = getStateDefinition().any();
            registerDefaultState(state);
        }

        @Override
        public boolean isPathfindable(BlockState state, PathComputationType type) {
            return ((config & CFG_AI_PASSABLE) != 0) && (super.isPathfindable(state, type));
        }

        @Override
        public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
            final boolean rsup = (state.hasBlockEntity() && (state.getBlock() != newState.getBlock()));
            super.onRemove(state, world, pos, newState, isMoving);
            if (rsup) world.updateNeighbourForOutputSignal(pos, this);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            final ServerLevel world = builder.getLevel();
            final Float explosion_radius = builder.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
            final BlockEntity te = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (!hasDynamicDropList()) return super.getDrops(state, builder);
            boolean is_explosion = (explosion_radius != null) && (explosion_radius > 0);
            return dropList(state, world, te, is_explosion);
        }

        @Override
        public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
            return super.propagatesSkylightDown(state, reader, pos);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos) {
            return state;
        }
    }

    public static class Cutout extends BaseBlock implements IStandardBlock {
        private final VoxelShape vshape;

        public Cutout(long conf, BlockBehaviour.Properties properties) {
            this(conf, properties, Auxiliaries.getPixeledAABB(0, 0, 0, 16, 16, 16));
        }

        public Cutout(long conf, BlockBehaviour.Properties properties, AABB aabb) {
            this(conf, properties, Shapes.create(aabb));
        }

        public Cutout(long conf, BlockBehaviour.Properties properties, VoxelShape voxel_shape) {
            super(conf, properties);
            vshape = voxel_shape;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext) {
            return vshape;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext) {
            return vshape;
        }

        @Override
        @Nullable
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return super.getStateForPlacement(context);
        }

        @Override
        public boolean isPossibleToRespawnInThis(BlockState state) {
            return false;
        }

        @Override
        public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
            return super.propagatesSkylightDown(state, reader, pos);
        }
    }
}