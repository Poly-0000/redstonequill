/*
 * @file StandardEntityBlocks.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Common functionality class for blocks with block entities.
 */
package wile.redstonepen.libmc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;


public class StandardEntityBlocks
{

    public static abstract class StandardBlockEntity extends BlockEntity
  {
    public StandardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    { super(type, pos, state); }

    public CompoundTag writenbt(HolderLookup.Provider hlp, CompoundTag nbt, boolean sync_packet)
    { return nbt; }

    public CompoundTag readnbt(HolderLookup.Provider hlp, CompoundTag nbt)
    { return nbt; }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider hlp)
    { readnbt(hlp, nbt); }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider hlp)
    { super.saveAdditional(writenbt(hlp, nbt, false), hlp); }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider hlp)
    { return writenbt(hlp, super.getUpdateTag(hlp), true); }

  }

}
