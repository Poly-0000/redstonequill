/*
 * @file Auxiliaries.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General commonly used functionality.
 */
package wile.redstonepen.libmc;

import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.slf4j.Logger;
import org.lwjgl.glfw.GLFW;

import wile.redstonepen.ModConstants;

import java.util.*;
import java.util.stream.Collectors;


public class Auxiliaries
{
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ModConstants.MODID);
  private static final String development_mode_control_file = ".redstonepen-dev";
  private static boolean development_mode = false;

  public static void init()
  {
    try {
      development_mode = new java.io.File(Auxiliaries.getGameDirectory().resolve(development_mode_control_file).toString()).isFile();
    } catch(Throwable ignored) {
    }
  }

  public static String modid()
  { return ModConstants.MODID; }

    public static java.nio.file.Path getGameDirectory()
  {
    // return FabricLoader.getInstance().getGameDir(); // Fabric
    return net.neoforged.fml.loading.FMLLoader.getGamePath();
  }

    public static boolean isDevelopmentMode()
  { return development_mode; }

    @OnlyIn(Dist.CLIENT)
  public static boolean isShiftDown()
  {
    return (InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
      InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT));
  }
  @OnlyIn(Dist.CLIENT)
  public static Optional<String> getClipboard()
  { return Optional.of(net.minecraft.client.gui.font.TextFieldHelper.getClipboardContents(net.minecraft.client.Minecraft.getInstance())); }

  @OnlyIn(Dist.CLIENT)
  public static boolean setClipboard(String text)
  { net.minecraft.client.gui.font.TextFieldHelper.setClipboardContents(net.minecraft.client.Minecraft.getInstance(), text); return true; }

    public static void logWarn(final String msg)
  { logger.warn(msg); }

  public static void logError(final String msg)
  { logger.error(msg); }

  /**
   * Text localization wrapper, implicitly prepends `MODID` to the
   * translation keys. Forces formatting argument, nullable if no special formatting shall be applied..
   */
  public static MutableComponent localizable(String modtrkey, Object... args)
  { return Component.translatable((modtrkey.startsWith("block.") || (modtrkey.startsWith("item."))) ? (modtrkey) : (modid()+"."+modtrkey), args); }

    public static MutableComponent localizable(String modtrkey)
  { return localizable(modtrkey, new Object[]{}); }

    @OnlyIn(Dist.CLIENT)
  public static String localize(String translationKey, Object... args)
  {
    final Component tr = Component.translatable(translationKey, args);
    tr.getStyle().applyFormat(ChatFormatting.RESET);
    return tr.getString().trim();
  }

  @OnlyIn(Dist.CLIENT)
  public static boolean hasTranslation(String key)
  { return net.minecraft.client.resources.language.I18n.exists(key); }

  @OnlyIn(Dist.CLIENT)
  public static List<Component> wrapText(Component text, int max_width_percent)
  {
    int max_width = ((Minecraft.getInstance().getWindow().getGuiScaledWidth())-10) * max_width_percent/100;
    return Minecraft.getInstance().font.getSplitter().splitLines(text, max_width, Style.EMPTY).stream()
      .map(ft->Component.literal(ft.getString()))
      .collect(Collectors.toList());
  }

    public static void playerChatMessage(final Player player, final String message)
  { player.displayClientMessage(Component.translatable(message.trim()), true); }

    public static AABB getPixeledAABB(double x0, double y0, double z0, double x1, double y1, double z1)
  { return new AABB(x0/16.0, y0/16.0, z0/16.0, x1/16.0, y1/16.0, z1/16.0); }

    public static AABB[] getRotatedAABB(AABB[] bb, Direction new_facing)
  { return getRotatedAABB(bb, new_facing, false); }

  public static AABB getRotatedAABB(AABB bb, Direction new_facing, boolean horizontal_rotation)
  {
    if(!horizontal_rotation) {
      switch(new_facing.get3DDataValue()) {
        case 0: return new AABB(1-bb.maxX,   bb.minZ,   bb.minY, 1-bb.minX,   bb.maxZ,   bb.maxY); // D
        case 1: return new AABB(1-bb.maxX, 1-bb.maxZ, 1-bb.maxY, 1-bb.minX, 1-bb.minZ, 1-bb.minY); // U
        case 2: return new AABB(  bb.minX,   bb.minY,   bb.minZ,   bb.maxX,   bb.maxY,   bb.maxZ); // N --> bb
        case 3: return new AABB(1-bb.maxX,   bb.minY, 1-bb.maxZ, 1-bb.minX,   bb.maxY, 1-bb.minZ); // S
        case 4: return new AABB(  bb.minZ,   bb.minY, 1-bb.maxX,   bb.maxZ,   bb.maxY, 1-bb.minX); // W
        case 5: return new AABB(1-bb.maxZ,   bb.minY,   bb.minX, 1-bb.minZ,   bb.maxY,   bb.maxX); // E
      }
    } else {
      switch(new_facing.get3DDataValue()) {
        case 0: return new AABB(  bb.minX, bb.minY,   bb.minZ,   bb.maxX, bb.maxY,   bb.maxZ); // D --> bb
        case 1: return new AABB(  bb.minX, bb.minY,   bb.minZ,   bb.maxX, bb.maxY,   bb.maxZ); // U --> bb
        case 2: return new AABB(  bb.minX, bb.minY,   bb.minZ,   bb.maxX, bb.maxY,   bb.maxZ); // N --> bb
        case 3: return new AABB(1-bb.maxX, bb.minY, 1-bb.maxZ, 1-bb.minX, bb.maxY, 1-bb.minZ); // S
        case 4: return new AABB(  bb.minZ, bb.minY, 1-bb.maxX,   bb.maxZ, bb.maxY, 1-bb.minX); // W
        case 5: return new AABB(1-bb.maxZ, bb.minY,   bb.minX, 1-bb.minZ, bb.maxY,   bb.maxX); // E
      }
    }
    return bb;
  }

  public static AABB[] getRotatedAABB(AABB[] bbs, Direction new_facing, boolean horizontal_rotation)
  {
    final AABB[] transformed = new AABB[bbs.length];
    for(int i=0; i<bbs.length; ++i) transformed[i] = getRotatedAABB(bbs[i], new_facing, horizontal_rotation);
    return transformed;
  }

  public static AABB getYRotatedAABB(AABB bb, int clockwise_90deg_steps)
  {
    final Direction[] direction_map = new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
    return getRotatedAABB(bb, direction_map[(clockwise_90deg_steps+4096) & 0x03], true);
  }

  public static AABB[] getYRotatedAABB(AABB[] bbs, int clockwise_90deg_steps)
  {
    final AABB[] transformed = new AABB[bbs.length];
    for(int i=0; i<bbs.length; ++i) transformed[i] = getYRotatedAABB(bbs[i], clockwise_90deg_steps);
    return transformed;
  }

  public static AABB getMirroredAABB(AABB bb, Direction.Axis axis)
  {
    return switch (axis) {
      case X -> new AABB(1 - bb.maxX, bb.minY, bb.minZ, 1 - bb.minX, bb.maxY, bb.maxZ);
      case Y -> new AABB(bb.minX, 1 - bb.maxY, bb.minZ, bb.maxX, 1 - bb.minY, bb.maxZ);
      case Z -> new AABB(bb.minX, bb.minY, 1 - bb.maxZ, bb.maxX, bb.maxY, 1 - bb.minZ);
    };
  }

  public static AABB[] getMirroredAABB(AABB[] bbs, Direction.Axis axis)
  {
    final AABB[] transformed = new AABB[bbs.length];
    for(int i=0; i<bbs.length; ++i) transformed[i] = getMirroredAABB(bbs[i], axis);
    return transformed;
  }

  public static VoxelShape getUnionShape(AABB ... aabbs)
  {
    VoxelShape shape = Shapes.empty();
    for(AABB aabb: aabbs) shape = Shapes.joinUnoptimized(shape, Shapes.create(aabb), BooleanOp.OR);
    return shape;
  }

}
