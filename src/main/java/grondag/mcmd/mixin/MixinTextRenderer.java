package grondag.mcmd.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.mcmd.McMdRenderer;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.texture.TextureManager;

@Mixin(TextRenderer.class)
public abstract class MixinTextRenderer implements McMdRenderer {
	@Shadow private TextureManager textureManager;
	@Shadow private FontStorage fontStorage;
	@Shadow private boolean rightToLeft;

	@Shadow
	protected abstract void drawGlyph(GlyphRenderer glyphRenderer_1, boolean boolean_1, boolean boolean_2, float float_1, float float_2, float float_3, BufferBuilder bufferBuilder_1, float float_4, float float_5, float float_6, float float_7);

	@Override
	public void mcmd_drawGlyph(GlyphRenderer glyphRenderer_1, boolean boolean_1, boolean boolean_2, float float_1, float float_2, float float_3, BufferBuilder bufferBuilder_1, float float_4, float float_5, float float_6, float float_7) {
		drawGlyph(glyphRenderer_1, boolean_1, boolean_2, float_1, float_2, float_3, bufferBuilder_1, float_4, float_5, float_6, float_7);
	}

	@Override
	public boolean mcmd_rightToLeft() {
		return rightToLeft;
	}

	@Override
	public FontStorage mcmd_fontStorage() {
		return fontStorage;
	}

	@Override
	public TextureManager mcmd_textureManager() {
		return textureManager;
	}
}