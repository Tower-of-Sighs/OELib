package com.mafuyu404.oelib.api.client.renderer;

import com.mafuyu404.oelib.client.renderer.FluidRenderers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * @deprecated
 * See {@link FluidRenderers}
 * <p>
 * 流体渲染器接口。
 * <p>
 * 可使用平铺的纹理 Sprite 将流体内容渲染到任意矩形区域内。
 * </p>
 *
 * <h3>类型参数</h3>
 * <ul>
 *   <li><b>T</b> — 平台流体引用类型。
 *       在 Fabric 上为：{@code net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant}；
 *       在 Forge 上为：{@code net.minecraftforge.fluids.FluidStack}。</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <ul>
 *   <li>调用 {@link #render(GuiGraphics, Object, long, long, int, int, int, int)} 方法，
 *       传入平台流体句柄、当前流体量与容量（用于垂直填充比例计算），以及目标渲染区域。</li>
 *   <li>如果你已经获取了 {@link TextureAtlasSprite} 和 ARGB 颜色，
 *       可直接调用 {@link #renderTiledSprite(GuiGraphics, TextureAtlasSprite, int, int, int, int, int, int)} 进行渲染。</li>
 * </ul>
 *
 * <h3>缩放逻辑</h3>
 * <p>
 * 垂直填充高度由 {@code amount/capacity} 的比例决定，并限制在 {@code [minHeight, fullHeight]} 范围内。
 * 如需自定义缩放规则，可使用 {@link #computeScaledHeight(long, long, int, int)} 方法。
 * </p>
 */
public interface IFluidRenderer<T> {

    /**
     * 将流体渲染到指定区域内。
     * <p>
     * 实现类应根据平台解析出纹理 Sprite 和 ARGB 着色颜色，
     * 根据 {@code amount/capacity} 计算缩放后的高度，并使用平铺四边形进行绘制。
     * </p>
     *
     * @param graphics    GUI 图形上下文（PoseStack 的封装）
     * @param fluidRef    平台流体引用（Fabric: FluidVariant，Forge: FluidStack）
     * @param amount      当前流体量（例如单位为 mB）
     * @param capacity    最大容量（单位需与 {@code amount} 一致）
     * @param x           目标区域左上角 X 坐标
     * @param y           目标区域左上角 Y 坐标
     * @param width       目标区域宽度（任意正整数）
     * @param height      目标区域高度（任意正整数）
     */
    void render(GuiGraphics graphics, T fluidRef, long amount, long capacity,
                int x, int y, int width, int height);

    /**
     * 使用指定的 Sprite 和 ARGB 颜色，在给定区域内绘制平铺填充。
     *
     * @param graphics     GUI 图形上下文
     * @param sprite       流体对应的纹理图集 Sprite（静止纹理）
     * @param colorARGB    ARGB 着色颜色（无需预乘 Alpha）
     * @param x            左上角 X 坐标
     * @param y            左上角 Y 坐标
     * @param width        目标宽度
     * @param scaledHeight 实际要填充的垂直高度（范围：0 到 fullHeight）
     * @param fullHeight   区域的最大垂直高度
     */
    void renderTiledSprite(GuiGraphics graphics, TextureAtlasSprite sprite, int colorARGB,
                           int x, int y, int width, int scaledHeight, int fullHeight);

    /**
     * 根据流体量与容量计算缩放后的垂直高度。
     *
     * @param amount     当前流体量
     * @param capacity   最大容量
     * @param fullHeight 目标区域的总高度
     * @param minHeight  当 amount > 0 时显示的最小像素高度
     * @return 缩放后的像素高度，范围限制在 [minHeight..fullHeight] 之间
     */
    default int computeScaledHeight(long amount, long capacity, int fullHeight, int minHeight) {
        return FluidRenderers.computeScaledHeight(amount, capacity, fullHeight, minHeight);
    }
}