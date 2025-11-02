/*
 * @file RsSignals.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General redstone signal related functionality.
 */
package wile.redstonepen.libmc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RsSignals {
    public static boolean canEmitWeakPower(BlockState state, Level world, BlockPos pos) {
        return state.isRedstoneConductor(world, pos);
    }
}
