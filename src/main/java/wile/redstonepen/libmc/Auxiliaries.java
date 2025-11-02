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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.slf4j.Logger;
import org.lwjgl.glfw.GLFW;

import wile.redstonepen.ModConstants;

import java.util.*;
import java.util.stream.Collectors;


public class Auxiliaries {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ModConstants.MODID);

    public static String modid() {
        return ModConstants.MODID;
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isShiftDown() {
        return (InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
                InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT));
    }

    @OnlyIn(Dist.CLIENT)
    public static Optional<String> getClipboard() {
        return Optional.of(net.minecraft.client.gui.font.TextFieldHelper.getClipboardContents(net.minecraft.client.Minecraft.getInstance()));
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean setClipboard(String text) {
        net.minecraft.client.gui.font.TextFieldHelper.setClipboardContents(net.minecraft.client.Minecraft.getInstance(), text);
        return true;
    }

    public static void logWarn(final String msg) {
        logger.warn(msg);
    }

    public static void logError(final String msg) {
        logger.error(msg);
    }

    /**
     * Text localization wrapper, implicitly prepends `MODID` to the
     * translation keys. Forces formatting argument, nullable if no special formatting shall be applied..
     */
    public static MutableComponent localizable(String modtrkey, Object... args) {
        return Component.translatable((modtrkey.startsWith("block.") || (modtrkey.startsWith("item."))) ? (modtrkey) : (modid() + "." + modtrkey), args);
    }

    @OnlyIn(Dist.CLIENT)
    public static String localize(String translationKey, Object... args) {
        final Component tr = Component.translatable(translationKey, args);
        tr.getStyle().applyFormat(ChatFormatting.RESET);
        return tr.getString().trim();
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasTranslation(String key) {
        return net.minecraft.client.resources.language.I18n.exists(key);
    }

    @OnlyIn(Dist.CLIENT)
    public static List<Component> wrapText(Component text, int max_width_percent) {
        int max_width = ((Minecraft.getInstance().getWindow().getGuiScaledWidth()) - 10) * max_width_percent / 100;
        return Minecraft.getInstance().font.getSplitter().splitLines(text, max_width, Style.EMPTY).stream()
                .map(ft -> Component.literal(ft.getString()))
                .collect(Collectors.toList());
    }

    public static AABB getPixeledAABB(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new AABB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
    }

    public static VoxelShape getUnionShape(AABB... aabbs) {
        VoxelShape shape = Shapes.empty();
        for (AABB aabb : aabbs) shape = Shapes.joinUnoptimized(shape, Shapes.create(aabb), BooleanOp.OR);
        return shape;
    }
}
