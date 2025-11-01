/*
 * @file StandardBlocks.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Common functionality class for decor blocks.
 */
package wile.redstonepen.libmc;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Function;

public class StandardBlocks
{
  public static final long CFG_DEFAULT                    = 0x0000000000000000L; // no special config
  public static final long CFG_WATERLOGGABLE              = 0x0000000000000008L; // The derived block extends IWaterLoggable
  public static final long CFG_HORIZIONTAL                = 0x0000000000000010L; // horizontal block, affects bounding box calculation at construction time and placement
  public static final long CFG_LOOK_PLACEMENT             = 0x0000000000000020L; // placed in direction the player is looking when placing.
  public static final long CFG_OPPOSITE_PLACEMENT         = 0x0000000000000080L; // placed in the opposite direction of the face the player clicked.
  public static final long CFG_FLIP_PLACEMENT_SHIFTCLICK  = 0x0000000000000200L; // placement direction flipped if player is sneaking
  public static final long CFG_AI_PASSABLE                = 0x0000000000000800L; // does not block movement path for AI, needed for non-opaque blocks with collision shapes not thin at the bottom or one side.

  public interface IStandardBlock
  {

    default boolean hasDynamicDropList()
    { return false; }

    default List<ItemStack> dropList(BlockState state, Level world, @Nullable BlockEntity te, boolean explosion)
    { return Collections.singletonList((!world.isClientSide()) ? (new ItemStack(state.getBlock().asItem())) : (ItemStack.EMPTY)); }

  }

  public static class BaseBlock extends Block implements IStandardBlock, SimpleWaterloggedBlock
  {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public final long config;

    public BaseBlock(long conf, BlockBehaviour.Properties properties)
    {
      super(properties);
      config = conf;
      BlockState state = getStateDefinition().any();
      if((conf & CFG_WATERLOGGABLE)!=0) state = state.setValue(WATERLOGGED, false);
      registerDefaultState(state);
    }
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag)
    { Auxiliaries.Tooltip.addInformation(stack, tooltip, flag, true); }

    @Override
    public boolean isPathfindable(BlockState state, PathComputationType type)
    { return ((config & CFG_AI_PASSABLE)!=0) && (super.isPathfindable(state, type)); }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving)
    {
      final boolean rsup = (state.hasBlockEntity() && (state.getBlock() != newState.getBlock()));
      super.onRemove(state, world, pos, newState, isMoving);
      if(rsup) world.updateNeighbourForOutputSignal(pos, this);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder)
    {
      final ServerLevel world = builder.getLevel();
      final Float explosion_radius = builder.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
      final BlockEntity te = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      if((!hasDynamicDropList()) || (world==null)) return super.getDrops(state, builder);
      boolean is_explosion = (explosion_radius!=null) && (explosion_radius > 0);
      return dropList(state, world, te, is_explosion);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
    { return (((config & CFG_WATERLOGGABLE)==0) || (!state.getValue(WATERLOGGED))) && super.propagatesSkylightDown(state, reader, pos); }

    @Override
    public FluidState getFluidState(BlockState state)
    { return (((config & CFG_WATERLOGGABLE)!=0) && state.getValue(WATERLOGGED)) ? Fluids.WATER.getSource(false) : super.getFluidState(state); }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
    {
      if(((config & CFG_WATERLOGGABLE)!=0) && (state.getValue(WATERLOGGED))) world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
      return state;
    }

    @Override // SimpleWaterloggedBlock
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter world, BlockPos pos, BlockState state, Fluid fluid)
    { return ((config & CFG_WATERLOGGABLE)!=0) && SimpleWaterloggedBlock.super.canPlaceLiquid(player, world, pos, state, fluid); }

    @Override // SimpleWaterloggedBlock
    public boolean placeLiquid(LevelAccessor world, BlockPos pos, BlockState state, FluidState fluidState)
    { return ((config & CFG_WATERLOGGABLE)!=0) && SimpleWaterloggedBlock.super.placeLiquid(world, pos, state, fluidState); }

    @Override // SimpleWaterloggedBlock
    public ItemStack pickupBlock(@Nullable Player player, LevelAccessor world, BlockPos pos, BlockState state)
    { return ((config & CFG_WATERLOGGABLE)!=0) ? (SimpleWaterloggedBlock.super.pickupBlock(player, world, pos, state)) : (ItemStack.EMPTY); }

    @Override // SimpleWaterloggedBlock
    public Optional<SoundEvent> getPickupSound()
    { return ((config & CFG_WATERLOGGABLE)!=0) ? (SimpleWaterloggedBlock.super.getPickupSound()) : Optional.empty(); }

  }

  public static class Cutout extends BaseBlock implements IStandardBlock
  {
    private final VoxelShape vshape;

    public Cutout(long conf, BlockBehaviour.Properties properties)
    { this(conf, properties, Auxiliaries.getPixeledAABB(0, 0, 0, 16, 16,16 )); }

    public Cutout(long conf, BlockBehaviour.Properties properties, AABB aabb)
    { this(conf, properties, Shapes.create(aabb)); }

    public Cutout(long conf, BlockBehaviour.Properties properties, AABB[] aabbs)
    { this(conf, properties, Arrays.stream(aabbs).map(Shapes::create).reduce(Shapes.empty(), (shape, aabb)->Shapes.joinUnoptimized(shape, aabb, BooleanOp.OR))); }

    public Cutout(long conf, BlockBehaviour.Properties properties, VoxelShape voxel_shape)
    { super(conf, properties); vshape = voxel_shape; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext)
    { return vshape; }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,  CollisionContext selectionContext)
    { return vshape; }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
      BlockState state = super.getStateForPlacement(context);
      if(state==null) return null;
      if((config & CFG_WATERLOGGABLE)!=0) {
        FluidState fs = context.getLevel().getFluidState(context.getClickedPos());
        state = state.setValue(WATERLOGGED,fs.getType()==Fluids.WATER);
      }
      return state;
    }

    @Override
    public boolean isPossibleToRespawnInThis(BlockState state)
    { return false; }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
    {
      if((config & CFG_WATERLOGGABLE)!=0) {
        if(state.getValue(WATERLOGGED)) return false;
      }
      return super.propagatesSkylightDown(state, reader, pos);
    }

    @Override
    public FluidState getFluidState(BlockState state)
    {
      if((config & CFG_WATERLOGGABLE)!=0) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
      }
      return super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
    {
      if((config & CFG_WATERLOGGABLE)!=0) {
        if(state.getValue(WATERLOGGED)) world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
      }
      return state;
    }
  }

  public static class WaterLoggable extends Cutout implements IStandardBlock
  {
    public WaterLoggable(long config, BlockBehaviour.Properties properties)
    { super(config|CFG_WATERLOGGABLE, properties); }

      @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(WATERLOGGED); }
  }

  public static class Directed extends Cutout implements IStandardBlock
  {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    protected final Map<BlockState,VoxelShape> vshapes;

    public Directed(long config, BlockBehaviour.Properties properties, final Function<List<BlockState>, Map<BlockState,VoxelShape>> shape_supplier)
    {
      super(config, properties);
      registerDefaultState(super.defaultBlockState().setValue(FACING, Direction.UP));
      vshapes = shape_supplier.apply(getStateDefinition().getPossibleStates());
    }

      @Override
    public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext)
    { return vshapes.get(state); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
    { return getShape(state, world, pos, selectionContext); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(FACING); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
      BlockState state = super.getStateForPlacement(context);
      if(state == null) return null;
      Direction facing = context.getClickedFace();
      if((config & (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) == (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) {
        // horizontal placement in direction the player is looking
        facing = context.getHorizontalDirection();
      } else if((config & (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) == (CFG_HORIZIONTAL)) {
        // horizontal placement on a face
        if(((facing==Direction.UP)||(facing==Direction.DOWN))) return null;
      } else if((config & CFG_LOOK_PLACEMENT)!=0) {
        // placement in direction the player is looking, with up and down
        facing = context.getNearestLookingDirection();
      }
      if((config & CFG_OPPOSITE_PLACEMENT)!=0) facing = facing.getOpposite();
      if(((config & CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0) && (context.getPlayer()!=null) &&  (context.getPlayer().isShiftKeyDown())) facing = facing.getOpposite();
      return state.setValue(FACING, facing);
    }
  }

}
