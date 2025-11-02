/*
 * @file ModContent.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 */
package wile.redstonepen;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import wile.redstonepen.blocks.*;
import wile.redstonepen.items.RedstonePenItem;
import wile.redstonepen.libmc.StandardBlocks;
import wile.redstonepen.libmc.Registries;

public class ModContent {
    public static void init() {
        initBlocks();
        initItems();
    }

    public static void initBlocks() {
        Registries.addBlock("track", () -> new RedstoneTrack.RedstoneTrackBlock(StandardBlocks.CFG_DEFAULT, BlockBehaviour.Properties.of().noCollission().instabreak().dynamicShape().randomTicks()),
                RedstoneTrack.TrackBlockEntity::new);
    }

    public static void initItems() {
        Registries.addItem("quill", () -> new RedstonePenItem((new Item.Properties()).stacksTo(1).durability(0)));
    }

    public static void initReferences() {
        references.TRACK_BLOCK = (RedstoneTrack.RedstoneTrackBlock) Registries.getBlock("track");
    }

    public static final class references {
        public static RedstoneTrack.RedstoneTrackBlock TRACK_BLOCK = null;
    }
}