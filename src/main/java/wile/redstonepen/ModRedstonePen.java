/*
 * @file ModRedstonePen.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 */
package wile.redstonepen;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;
import wile.redstonepen.blocks.RedstoneTrack;
import wile.redstonepen.libmc.Auxiliaries;
import wile.redstonepen.libmc.Registries;

@Mod("redstonepen")
public class ModRedstonePen
{
  public ModRedstonePen(IEventBus bus)
  {
    Auxiliaries.init();
    ModContent.init();
    bus.addListener(LiveCycleEvents::onRegister);
    bus.addListener(LiveCycleEvents::onRegisterNetwork);
    bus.addListener(ModRedstonePen::onBuildCreativeTabContents);
  }
    // TODO: Add Jade support
    // TODO: Make work with shaders (emissive texture)
    // TODO: Make redstone unplacable with config
    // TODO: Check why redstone is not updating properly
    // TODO: Color code lines
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(Registries.getItem("quill"));
        }
    }
  private static class LiveCycleEvents
  {
    private static void onRegister(RegisterEvent event)
    {
      final String registry_name = event.getRegistry().key().location().toString();
      if(!registry_name.equals("minecraft:block")) return;
      Registries.instantiateAll();
      ModContent.initReferences();
    }
    private static void onRegisterNetwork(final RegisterPayloadHandlersEvent event)
    {
      PayloadRegistrar registrar = event.registrar("v1");
      wile.redstonepen.libmc.Networking.init(registrar);
    }
  }
  @EventBusSubscriber(modid=ModConstants.MODID, value=Dist.CLIENT)
  public static class ClientEvents
  {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event)
    {
      BlockEntityRenderers.register((BlockEntityType<RedstoneTrack.TrackBlockEntity>)Registries.getBlockEntityTypeOfBlock("track"), wile.redstonepen.detail.ModRenderers.TrackTer::new);
    }
    @SubscribeEvent
    public static void onRegisterModels(final ModelEvent.RegisterAdditional event)
    {
      wile.redstonepen.detail.ModRenderers.TrackTer.registerModels().forEach(event::register);
    }
  }

}
