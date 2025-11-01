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
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;
import wile.redstonepen.blocks.RedstoneTrack;
import wile.redstonepen.libmc.Auxiliaries;
import wile.redstonepen.libmc.Networking;
import wile.redstonepen.libmc.Overlay;
import wile.redstonepen.libmc.Registries;

@Mod("redstonepen")
public class ModRedstonePen
{
  public ModRedstonePen(IEventBus bus)
  {
    Auxiliaries.init();
    Auxiliaries.logGitVersion();
    Registries.init();
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
            event.accept(Registries.getItem("pen"));
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
      wile.redstonepen.libmc.NetworkingClient.clientInit();
    }
  }
  @EventBusSubscriber(modid=ModConstants.MODID, value=Dist.CLIENT)
  public static class ClientEvents
  {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event)
    {
      Networking.OverlayTextMessage.setHandler(Overlay.TextOverlayGui::show);
      Overlay.on_config(0.75);
      BlockEntityRenderers.register((BlockEntityType<RedstoneTrack.TrackBlockEntity>)Registries.getBlockEntityTypeOfBlock("track"), wile.redstonepen.detail.ModRenderers.TrackTer::new);
    }
    @SubscribeEvent
    public static void onRegisterModels(final ModelEvent.RegisterAdditional event)
    {
      wile.redstonepen.detail.ModRenderers.TrackTer.registerModels().forEach(event::register);
    }
  }
  @EventBusSubscriber(modid=ModConstants.MODID, value=Dist.CLIENT)
  public static class ClientGameEvents
  {
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event)
    {
      Overlay.TextOverlayGui.INSTANCE.onRenderGui(event.getGuiGraphics());
    }
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderWorldOverlay(net.neoforged.neoforge.client.event.RenderLevelStageEvent event)
    {
      if(event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS) {
        Overlay.TextOverlayGui.INSTANCE.onRenderWorldOverlay(event.getPoseStack(), event.getRenderTick());
      }
    }
  }
}
