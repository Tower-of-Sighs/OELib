package cc.sighs.oelib.renderer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Encapsulates the visual properties required to render a fluid in the UI.
 *
 * @param sprite     The texture sprite (usually the "still" texture) from the block atlas.
 * @param colorARGB  The ARGB color tint to apply to the texture.
 */
public record FluidRenderAttributes(TextureAtlasSprite sprite, int colorARGB) {
}