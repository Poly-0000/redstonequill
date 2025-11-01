/*
 * @file Tooltip.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Delayed tooltip for a selected area. Constructed with a
 * GUI, invoked in `render()`.
 */
package wile.redstonepen.libmc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class TooltipDisplay
{
  private static long default_delay = 450;
  private static int default_max_deviation = 1;
  public static class TipRange
  {
    public final int x0,y0,x1,y1;
    public final Supplier<Component> text;
    public TipRange(int x, int y, int w, int h, Supplier<Component> text){
        this.text=text; this.x0=x; this.y0=y; this.x1=x0+w-1; this.y1=y0+h-1;
    }
  }
  private List<TipRange> ranges = new ArrayList<>();
  private long delay = default_delay;
  private int max_deviation = default_max_deviation;
  private int x_last, y_last;
  private long t;
  private final Font font;
  private static boolean had_render_exception = false;
  public TooltipDisplay(){
      t = System.currentTimeMillis(); this.font = Minecraft.getInstance().font;
  }
    public void resetTimer(){
      t = System.currentTimeMillis();
  }
  public boolean render(GuiGraphics gg, int x, int y)
  {
    if(had_render_exception) return false;
    if((Math.abs(x-x_last) > max_deviation) || (Math.abs(y-y_last) > max_deviation)) {
      x_last = x;
      y_last = y;
      resetTimer();
      return false;
    } else if(Math.abs(System.currentTimeMillis()-t) < delay) {
      return false;
    } else if(ranges.stream().noneMatch(
      (tip)->{
        if((x<tip.x0) || (x>tip.x1) || (y<tip.y0) || (y>tip.y1)) return false;
        final Component tip_component = tip.text.get();
        if(tip_component.getString().isEmpty()) return false;
        try {
          final List<Component> lines = Auxiliaries.wrapText(tip_component, 80);
          gg.renderTooltip(this.font, lines, Optional.empty(), x, y);
        } catch(Exception ex) {
          had_render_exception = true;
          Auxiliaries.logError("Tooltip rendering disabled due to exception: '" + ex.getMessage() + "'");
          return false;
        }
        return true;
      })
    ){
      resetTimer();
      return false;
    } else {
      return true;
    }
  }
}
