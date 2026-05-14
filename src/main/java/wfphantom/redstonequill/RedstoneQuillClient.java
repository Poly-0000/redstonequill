package wfphantom.redstonequill;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import wfphantom.redstonequill.blocks.RedstoneTrack;
import wfphantom.redstonequill.detail.ModRenderers;
import wfphantom.redstonequill.libmc.Registries;

@Mod(value = RedstoneQuill.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = RedstoneQuill.MODID, value = Dist.CLIENT)
public class RedstoneQuillClient {
    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onClientSetup(final FMLClientSetupEvent event) {
        BlockEntityRenderers.register((BlockEntityType<RedstoneTrack.TrackBlockEntity>) Registries.getBlockEntityTypeOfBlock("track"), renderer -> new ModRenderers.TrackTer());
    }

    @SubscribeEvent
    public static void onRegisterModels(final ModelEvent.RegisterAdditional event) {
        ModRenderers.TrackTer.registerModels().forEach(event::register);
    }
}
