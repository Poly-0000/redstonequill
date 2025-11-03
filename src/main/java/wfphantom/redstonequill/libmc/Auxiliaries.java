/*
 * @file Auxiliaries.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General commonly used functionality.
 */
package wfphantom.redstonequill.libmc;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.slf4j.Logger;

import wfphantom.redstonequill.ModConstants;

public class Auxiliaries {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ModConstants.MODID);

    public static void logWarn(final String msg) {
        logger.warn(msg);
    }

    public static void logError(final String msg) {
        logger.error(msg);
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

    public static AABB getPixeledAABB(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new AABB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
    }

    public static VoxelShape getUnionShape(AABB... aabbs) {
        VoxelShape shape = Shapes.empty();
        for (AABB aabb : aabbs) shape = Shapes.joinUnoptimized(shape, Shapes.create(aabb), BooleanOp.OR);
        return shape;
    }
}
