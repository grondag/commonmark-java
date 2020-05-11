package grondag.mcmd;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.GlyphRenderer.Rectangle;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

import grondag.fonthack.ext.RenderableGlyphExt;
import grondag.fonthack.ext.TextRendererExt;

public class McMdRenderer {
	//	char ESC = '§';
	//
	public static final char BOLD = (char) 0xE000;
	public static final char STRIKETHROUGH = (char) 0xE001;
	public static final char UNDERLINE = (char) 0xE002;
	public static final char ITALIC = (char) 0xE003;
	public static final char INDENT_PLUS = (char) 0xE004;

	public static final char BOLD_OFF = (char) 0xE005;
	public static final char STRIKETHROUGH_OFF = (char) 0xE006;
	public static final char UNDERLINE_OFF = (char) 0xE007;
	public static final char ITALIC_OFF = (char) 0xE008;
	public static final char INDENT_MINUS = (char) 0xE009;

	public static final char NEWLINE  = (char) 0xE00A	;
	public static final char NEWLINE_PLUS_HALF  = (char) 0xE00B;

	/** No closing tag - resets x coordinate to current indentation level. */
	public static final char ALIGN_TO_INDENT = (char) 0xE00C;

	public static final char NOTHING = (char) 0xE00D;


	public static final String STR_BOLD = Character.toString(BOLD);
	public static final String STR_STRIKETHROUGH = Character.toString(STRIKETHROUGH);
	public static final String STR_UNDERLINE = Character.toString(UNDERLINE);
	public static final String STR_ITALIC = Character.toString(ITALIC);
	public static final String STR_INDENT_PLUS = Character.toString(INDENT_PLUS);

	public static final String STR_BOLD_OFF = Character.toString(BOLD_OFF);
	public static final String STR_STRIKETHROUGH_OFF = Character.toString(STRIKETHROUGH_OFF);
	public static final String STR_UNDERLINE_OFF = Character.toString(UNDERLINE_OFF);
	public static final String STR_ITALIC_OFF = Character.toString(ITALIC_OFF);
	public static final String STR_INDENT_MINUS = Character.toString(INDENT_MINUS);

	public static final String STR_ALIGN_TO_INDENT = Character.toString(ALIGN_TO_INDENT);
	public static final String STR_NEWLINE = Character.toString(NEWLINE);
	public static final String STR_HALF_NEWLINE = Character.toString(NEWLINE_PLUS_HALF);

	private final TextRenderer baseRenderer;

	private final FontAdapter adapter;
	private final List<Rectangle> rects = Lists.newArrayList();
	protected final FontStorage fontStorage;

	final McMdStyle style;

	public McMdRenderer(
			McMdStyle style,
			TextRenderer baseRenderer)
	{
		this.style = style;
		this.baseRenderer = baseRenderer;
		fontStorage = ((TextRendererExt) baseRenderer).ext_fontStorage();
		//		adapter = ((TextRendererExt) baseRenderer).ext_fontStorage().getGlyph('a') instanceof RenderableGlyphExt
		//			? new TrueTypeAdapter() : new StandardAdapter();

		adapter = new TrueTypeAdapter();
	}

	class LineBreaker {
		int indent = 0;
		float margin = 0;
		int bold = 0;
		int italic = 0;
		final float indentWidth = adapter.indentWidth;
		final float lineHeight = style.lineHeight;
		final int width;

		LineBreaker(int width) {
			this.width = Math.max(1, width);
		}

		int breakLine(String text) {
			final int len = text.length();
			float w = 0.0F;
			int i = 0;
			int lastSpace = -1;

			char kernChar = NOTHING;

			for(boolean singleOrEmpty = true; i < len; ++i) {
				final char c = text.charAt(i);

				switch(c) {
				case BOLD:
					if(bold++ == 0) {
						kernChar = NOTHING;
					}
					break;

				case BOLD_OFF:
					if(--bold == 0) {
						kernChar = NOTHING;
					}
					break;

				case ITALIC:
					if(italic++ == 0) {
						kernChar = NOTHING;
						w += style.italicSpace;
					}
					break;

				case ITALIC_OFF:
					if(--italic == 0) {
						kernChar = NOTHING;
						w += style.italicSpace;
					}
					break;

				case INDENT_PLUS:
					margin = ++indent * indentWidth;
					break;

				case INDENT_MINUS:
					margin = --indent * indentWidth;
					break;

				case ALIGN_TO_INDENT:
					//because margin is always added, resetting to margin is resetting to zero
					kernChar = NOTHING;
					w = 0;
					break;

				case NEWLINE:
				case NEWLINE_PLUS_HALF:
					return i + 1;

				case ' ':
					lastSpace = i;
					w  += style.space;
					kernChar = NOTHING;
					break;

				default:
					break;
				}

				if (w != 0.0F) {
					singleOrEmpty = false;
				}

				w += (kernChar == NOTHING ? adapter.getCharWidth(c, bold > 0, italic > 0, lineHeight) : adapter.getCharWidth(c, kernChar, bold > 0, italic > 0, lineHeight));

				if (w + margin > width) {
					if (singleOrEmpty) {
						++i;
					}
					break;
				}
			}

			return i != len && lastSpace != -1 && lastSpace < i ? lastSpace : i;
		}
	}

	public List<String> wrapMarkdownToWidth(String markdown, int width, @Nullable List<String> lines) {
		if (lines == null)  {
			lines = new ArrayList<>();
		}

		String line;

		final LineBreaker breaker = new LineBreaker(width);

		for(; !markdown.isEmpty(); lines.add(line)) {
			final int lineWidth = breaker.breakLine(markdown);

			if (markdown.length() <= lineWidth) {
				lines.add(markdown);
				break;
			}

			line = markdown.substring(0, lineWidth);
			final char endChar = markdown.charAt(lineWidth);
			final boolean whitespaceEnd = endChar == ' ';
			markdown = markdown.substring(lineWidth + (whitespaceEnd ? 1 : 0));
		}

		return lines;
	}

	abstract class FontAdapter {
		final Tessellator tess = Tessellator.getInstance();
		final float indentWidth = ((TextRendererExt) baseRenderer).ext_fontStorage().getGlyph(' ').getAdvance() * 5;
		protected Identifier lastGlyphTexture = null;

		abstract float draw(char c, char kernChar, boolean bold, boolean italic, float x, float y, float height, Matrix4f matrix4f, VertexConsumerProvider vertexConsumerProvider, float red, float green, float blue, float alpha, int light);

		protected abstract float getCharWidth(char kernChar, char c, boolean bold, boolean italic, float height);

		protected float getCharWidth(char c, boolean bold, boolean italic, float height) {
			return fontStorage.getGlyph(c).getAdvance(bold);
		}

		protected void reset() {
			lastGlyphTexture = null;
		}
	}

	class StandardAdapter extends FontAdapter {

		@Override
		public float draw(char c, char kernChar, boolean bold, boolean italic, float x, float y, float height, Matrix4f matrix4f, VertexConsumerProvider vertexConsumerProvider, float red, float green, float blue, float alpha, int light) {


			//TODO: will need a custom renderlayer here to get correct blending
			final Glyph glyph = fontStorage.getGlyph(c);
			final GlyphRenderer glyphRenderer = fontStorage.getGlyphRenderer(c);
			final VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(glyphRenderer.method_24045(false));
			//			final Identifier glyphTexture = glyphRenderer.getId();

			//			if (glyphTexture != null) {
			//				if (lastGlyphTexture != glyphTexture) {
			//					tess.draw();
			//					textureManager.bindTexture(glyphTexture);
			//					textureManager.getTexture(glyphTexture).setFilter(true, true);
			//					GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
			//					buffer.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
			//					lastGlyphTexture = glyphTexture;
			//				}

			glyphRenderer.draw(italic, x, y, matrix4f, vertexConsumer, red, green, blue, alpha, light);
			if (bold) {
				glyphRenderer.draw(italic, x + glyph.getBoldOffset(), y, matrix4f, vertexConsumer, red, green, blue, alpha, light);
			}
			//			}

			return glyph.getAdvance(bold);
		}

		@Override
		protected float getCharWidth(char kernChar, char c, boolean bold, boolean italic, float height) {
			return fontStorage.getGlyph(c).getAdvance(bold);
		}
	}

	class TrueTypeAdapter extends FontAdapter {

		@Override
		public float draw(char c, char kernChar, boolean bold, boolean italic, float x, float y, float height, Matrix4f matrix4f, VertexConsumerProvider vertexConsumerProvider, float red, float green, float blue, float alpha, int light) {
			final Glyph glyph = fontStorage.getGlyph(c);
			final GlyphRenderer glyphRenderer = fontStorage.getGlyphRenderer(c);
			final VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(glyphRenderer.method_24045(false));
			//			final Identifier glyphTexture = glyphRenderer.getId();

			final float kerning;
			if (kernChar == NOTHING) {
				kerning = 0;
			} else {
				final RenderableGlyphExt gx = (RenderableGlyphExt) glyph;
				kerning = gx.kerning(kernChar);
				x += kerning;
			}

			//			if (glyphTexture != null) {
			//				if (lastGlyphTexture != glyphTexture) {
			//					tess.draw();
			//					textureManager.bindTexture(glyphTexture);
			//					textureManager.getTexture(glyphTexture).setFilter(true, true);
			//					GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
			//					buffer.begin(7, VertexFormats.POSITION_UV_COLOR);
			//					lastGlyphTexture = glyphTexture;
			//				}

			glyphRenderer.draw(italic, x, y, matrix4f, vertexConsumer, red, green, blue, alpha, light);

			if (bold) {
				glyphRenderer.draw(italic, x + glyph.getBoldOffset(), y, matrix4f, vertexConsumer, red, green, blue, alpha, light);
			}
			//			}

			return glyph.getAdvance() + kerning;
		}

		@Override
		protected float getCharWidth(char kernChar, char c, boolean bold, boolean italic, float height) {
			final Glyph glyph = fontStorage.getGlyph(c);

			if (kernChar == NOTHING) {
				return glyph.getAdvance(bold);
			} else {
				final RenderableGlyphExt gx = (RenderableGlyphExt) glyph;
				return glyph.getAdvance(bold) + gx.kerning(kernChar);
			}
		}
	}

	public void drawMarkdown(Matrix4f matrix4f, VertexConsumerProvider vertexConsumerProvider, List<String> lines, float x, float y, int color, float yOffset, float height, int light) {
		GlStateManager.enableAlphaTest();

		if (lines == null || lines.isEmpty()) {
			return;
		} else {
			if ((color & 0xFC000000) == 0) {
				color |= 0xFF000000;
			}

			// TODO: will need to be moved to custom render layer
			//			GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
			//			GlStateManager.alphaFunc(516, 0.1F);
			//			GlStateManager.enableBlend();
			adapter.reset();
			drawMarkdownInner(matrix4f, vertexConsumerProvider, lines, x, y, color, yOffset, height, light);
			//			GlStateManager.disableBlend();
		}
	}

	public void drawMarkdownInner(Matrix4f matrix4f,VertexConsumerProvider vertexConsumerProvider, List<String> lines, float x, final float yIn, int color, float yOffset, float height, int light) {
		final boolean rightToLeft = baseRenderer.isRightToLeft();
		final float baseX = x;
		final float baseRed = ((color >> 16) & 255) / 255.0F;
		final float baseGreen = ((color >> 8) & 255) / 255.0F;
		final float baseBlue = (color & 255) / 255.0F;
		final float red = baseRed;
		final float green = baseGreen;
		final float blue = baseBlue;
		final float alpha = (color >> 24 & 255) / 255.0F;
		//		final Tessellator tess = Tessellator.getInstance();
		//		final BufferBuilder buff = tess..getBuffer();
		final float yMax = yIn + height;
		final float singleLine = style.lineHeight;
		final float singleLinePlus = style.lineHeightPlusHalf;
		final float indentWidth = adapter.indentWidth;
		final List<Rectangle> rects = this.rects;
		rects.clear();

		float y = yIn - yOffset;
		int bold = 0;
		int italic = 0;
		int underline = 0;
		int strikethru = 0;
		int indent = 0;
		float margin = 0;
		char kernChar = NOTHING;
		float lineHeight = singleLine;

		//		vertexConsumer.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);

		for (String text : lines) {
			if (rightToLeft) {
				text = baseRenderer.mirror(text);
			}

			for(int i = 0; i < text.length(); ++i) {
				final char c = text.charAt(i);

				switch(c) {

				case BOLD:
					if(bold++ == 0) {
						kernChar = NOTHING;
					}
					break;

				case BOLD_OFF:
					if(--bold == 0) {
						kernChar = NOTHING;
					}
					break;

				case STRIKETHROUGH:
					++strikethru;
					break;

				case STRIKETHROUGH_OFF:
					--strikethru;
					break;

				case UNDERLINE:
					++underline;
					break;

				case UNDERLINE_OFF:
					--underline;
					break;

				case ITALIC:
					if(italic++ == 0) {
						kernChar = NOTHING;
						x += style.italicSpace;
					}
					break;

				case ITALIC_OFF:
					if(--italic == 0) {
						kernChar = NOTHING;
						x += style.italicSpace;
					}
					break;

				case INDENT_PLUS:
					++indent;
					margin = indent * indentWidth * (baseRenderer.isRightToLeft() ? -1 : 1);
					break;

				case INDENT_MINUS:
					--indent;
					margin = indent * indentWidth * (baseRenderer.isRightToLeft() ? -1 : 1);
					break;

				case ALIGN_TO_INDENT:
					//because margin is always added, resetting to margin is resetting to base
					kernChar = NOTHING;
					x = baseX;
					break;

				case NEWLINE:
					lineHeight = singleLine;
					kernChar = NOTHING;
					break;

				case NEWLINE_PLUS_HALF:
					lineHeight = singleLinePlus;
					kernChar = NOTHING;
					break;

				default:
					if (y >= yIn && y + lineHeight <= yMax) {

						final float advance = c == ' ' ? style.space : adapter.draw(c, kernChar, bold > 0, italic > 0, margin + x, y, lineHeight, matrix4f, vertexConsumerProvider, red, green, blue, alpha, light);

						if (strikethru > 0) {
							rects.add(new Rectangle(margin + x, y + style.strikethroughY, margin + x + advance, y + style.strikethroughY - style.lineThickness, -0.01F, red, green, blue, alpha));
						}

						if (underline > 0) {
							rects.add(new Rectangle(margin + x, y + style.underlineY, margin + x + advance, y + style.underlineY - style.lineThickness, -0.01F, red, green, blue, alpha));
						}

						x += advance;
						kernChar = c;
					}
				}
			}

			// PERF: early skip/exit for Y out of range
			y += lineHeight;
			kernChar = NOTHING;
			lineHeight = singleLine;
			x = baseX;
		}

		if (!rects.isEmpty()) {
			final GlyphRenderer gr = fontStorage.getRectangleRenderer();
			final VertexConsumer vc = vertexConsumerProvider.getBuffer(gr.method_24045(false));

			for (final Rectangle r :rects) {
				gr.drawRectangle(r, matrix4f, vc, light);
			}
		}
	}

	/**
	 * Returns total vertical height of lines that have already been split to a list.
	 */
	public float verticalHeight(List<String> lines) {
		float y = 0;

		final float singleLine = style.lineHeight;
		final float singleLinePlus = style.lineHeightPlusHalf;
		float lineHeight = singleLine;
		for (final String text : lines) {
			for(int i = 0; i < text.length(); ++i) {
				final char c = text.charAt(i);

				switch(c) {

				case NEWLINE:
					lineHeight = singleLine;
					break;

				case NEWLINE_PLUS_HALF:
					lineHeight = singleLinePlus;
					break;

				default:
					break;
				}
			}

			y += lineHeight;
			lineHeight = singleLine;
		}

		return y;
	}

	//	static class Rectangle {
	//		protected final float xMin;
	//		protected final float yMin;
	//		protected final float xMax;
	//		protected final float yMax;
	//		protected final float red;
	//		protected final float green;
	//		protected final float blue;
	//		protected final float alpha;
	//
	//		private Rectangle(float xMin, float yMin, float xMax, float yMax, float red, float green, float blue, float alpha) {
	//			this.xMin = xMin;
	//			this.yMin = yMin;
	//			this.xMax = xMax;
	//			this.yMax = yMax;
	//			this.red = red;
	//			this.green = green;
	//			this.blue = blue;
	//			this.alpha = alpha;
	//		}
	//
	//		public void draw(BufferBuilder buffer) {
	//			buffer.vertex(xMin, yMin, 0.0D).color(red, green, blue, alpha).next();
	//			buffer.vertex(xMax, yMin, 0.0D).color(red, green, blue, alpha).next();
	//			buffer.vertex(xMax, yMax, 0.0D).color(red, green, blue, alpha).next();
	//			buffer.vertex(xMin, yMax, 0.0D).color(red, green, blue, alpha).next();
	//		}
	//	}
}
