package grondag.mcmd;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;

import grondag.fonthack.TextRendererExt;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public class McMdRenderer {
	//	char ESC = '§';
	//
	//	char BOLD = (char) 0xE000;
	//	char STRIKETHROUGH = (char) 0xE001;
	//	char UNDERLINE = (char) 0xE002;
	//	char ITALIC = (char) 0xE003;
	//	char INDENT_PLUS = (char) 0xE004;
	//
	//	char BOLD_OFF = (char) 0xE005;
	//	char STRIKETHROUGH_OFF = (char) 0xE006;
	//	char UNDERLINE_OFF = (char) 0xE007;
	//	char ITALIC_OFF = (char) 0xE008;
	//	char INDENT_MINUS = (char) 0xE009;
	//
	//	char NEWLINE  = (char) 0xE00A	;
	//	char NEWLINE_PLUS_HALF  = (char) 0xE00B;
	//
	//	/** No closing tag - resets x coordinate to current indentation level. */
	//	char ALIGN_TO_INDENT = (char) 0xE00C;
	//
	//	String STR_ESC = Character.toString(ESC);
	//
	//	String STR_BOLD = Character.toString(BOLD);
	//	String STR_STRIKETHROUGH = Character.toString(STRIKETHROUGH);
	//	String STR_UNDERLINE = Character.toString(UNDERLINE);
	//	String STR_ITALIC = Character.toString(ITALIC);
	//	String STR_INDENT_PLUS = Character.toString(INDENT_PLUS);
	//
	//	String STR_BOLD_OFF = Character.toString(BOLD_OFF);
	//	String STR_STRIKETHROUGH_OFF = Character.toString(STRIKETHROUGH_OFF);
	//	String STR_UNDERLINE_OFF = Character.toString(UNDERLINE_OFF);
	//	String STR_ITALIC_OFF = Character.toString(ITALIC_OFF);
	//	String STR_INDENT_MINUS = Character.toString(INDENT_MINUS);
	//
	//	String STR_ALIGN_TO_INDENT = Character.toString(ALIGN_TO_INDENT);
	//	String STR_NEWLINE = Character.toString(NEWLINE);
	//	String STR_HALF_NEWLINE = Character.toString(NEWLINE_PLUS_HALF);

	public static final char ESC = '§';

	public static final char OBFUSCATE = 'o';
	public static final char BOLD = 'b';
	public static final char STRIKETHROUGH = 's';
	public static final char UNDERLINE = 'u';
	public static final char ITALIC = 'i';
	public static final char INDENT_PLUS = 't';

	public static final char OBFUSCATE_OFF = 'O';
	public static final char BOLD_OFF = 'B';
	public static final char STRIKETHROUGH_OFF = 'S';
	public static final char UNDERLINE_OFF = 'U';
	public static final char ITALIC_OFF = 'I';
	public static final char INDENT_MINUS = 'T';

	public static final char NEWLINE  = 'n';
	public static final char NEWLINE_PLUS_HALF  = 'h';

	/** No closing tag - resets x coordinate to current indentation level. */
	public static final char ALIGN_TO_INDENT = 'a';

	public static final String ESC_STR = Character.toString(ESC);

	public static final String ESC_BOLD = ESC_STR + Character.toString(BOLD);
	public static final String ESC_STRIKETHROUGH = ESC_STR + Character.toString(STRIKETHROUGH);
	public static final String ESC_UNDERLINE = ESC_STR + Character.toString(UNDERLINE);
	public static final String ESC_ITALIC = ESC_STR + Character.toString(ITALIC);
	public static final String ESC_OBFUSCATE = ESC_STR + Character.toString(OBFUSCATE);
	public static final String ESC_INDENT_PLUS = ESC_STR + Character.toString(INDENT_PLUS);

	public static final String ESC_BOLD_OFF = ESC_STR + Character.toString(BOLD_OFF);
	public static final String ESC_STRIKETHROUGH_OFF = ESC_STR + Character.toString(STRIKETHROUGH_OFF);
	public static final String ESC_UNDERLINE_OFF = ESC_STR + Character.toString(UNDERLINE_OFF);
	public static final String ESC_ITALIC_OFF = ESC_STR + Character.toString(ITALIC_OFF);
	public static final String ESC_OBFUSCATE_OFF = ESC_STR + Character.toString(OBFUSCATE_OFF);
	public static final String ESC_INDENT_MINUS = ESC_STR + Character.toString(INDENT_MINUS);

	public static final String ESC_ALIGN_TO_INDENT = ESC_STR + Character.toString(ALIGN_TO_INDENT);
	public static final String ESC_NEWLINE = ESC_STR + Character.toString(NEWLINE);
	public static final String ESC_HALF_NEWLINE = ESC_STR + Character.toString(NEWLINE_PLUS_HALF);

	private final TextRenderer baseRenderer;
	private final TextRenderer italicRenderer;
	private final TextRenderer boldRenderer;
	private final TextRenderer boldItalicRenderer;

	public McMdRenderer(
		TextRenderer baseRenderer,
		TextRenderer italicRenderer,
		TextRenderer boldRenderer,
		TextRenderer boldItalicRenderer)
	{
		this.baseRenderer = baseRenderer;
		this.italicRenderer = italicRenderer;
		this.boldRenderer = boldRenderer;
		this.boldItalicRenderer = boldItalicRenderer;
	}

	public String wrapMarkdownToWidth(String markdown, int width) {
		// TODO
		return "";
	}

	public int characterCountForWidth(String text, int width) {
		width = Math.max(1, width);
		final int len = text.length();
		float w = 0.0F;
		int i = 0;
		int lastSpace = -1;
		int indent = 0;
		float margin = 0;
		int boldCount = 0;
		final float indentWidth = ((TextRendererExt) baseRenderer).ext_fontStorage().getGlyph(' ').getAdvance() * 5;

		for(boolean singleOrEmpty = true; i < len; ++i) {
			final char c = text.charAt(i);

			if (c == ESC && i + 1 < text.length()) {
				switch(text.charAt(++i)) {
				case BOLD:
					++boldCount;
					break;

				case BOLD_OFF:
					--boldCount;
					break;

				case INDENT_PLUS:
					++indent;
					margin = indent * indentWidth;
					break;

				case INDENT_MINUS:
					--indent;
					margin = indent * indentWidth;
					break;

				case ALIGN_TO_INDENT:
					//because margin is always added, resetting to margin is resetting to zero
					w = 0;
					break;

				case NEWLINE:
				case NEWLINE_PLUS_HALF:
					return i + 1;

				default:
					break;
				}
			}

			if (c == ' ') {
				lastSpace = i;
			}

			if (w != 0.0F) {
				singleOrEmpty = false;
			}

			w += baseRenderer.getCharWidth(c);

			if (boldCount > 0) {
				++w;
			}

			if (w + margin > width) {
				if (singleOrEmpty) {
					++i;
				}
				break;
			}
		}

		return i != len && lastSpace != -1 && lastSpace < i ? lastSpace : i;
	}

	public List<String> wrapMarkdownToWidthAsList(String markdown, int width, @Nullable List<String> target) {
		if (target == null)  {
			target = new ArrayList<>();
		}

		String line;
		for(; !markdown.isEmpty(); target.add(line)) {
			final int lineWidth = characterCountForWidth(markdown, width);

			if (markdown.length() <= lineWidth) {
				target.add(markdown);
				break;
			}

			line = markdown.substring(0, lineWidth);
			final char endChar = markdown.charAt(lineWidth);
			final boolean whitespaceEnd = endChar == ' ';
			markdown = markdown.substring(lineWidth + (whitespaceEnd ? 1 : 0));
		}

		return target;
	}

	public void drawGlyph(GlyphRenderer glyphRenderer, boolean bold, boolean italic, float offset, float x, float y, BufferBuilder buffer, float red, float green, float blue, float alpha) {
		final TextureManager tm = ((TextRendererExt) baseRenderer).ext_textureManager();
		glyphRenderer.draw(tm, italic, x, y, buffer, red, green, blue, alpha);
		if (bold) {
			glyphRenderer.draw(tm, italic, x + offset, y, buffer, red, green, blue, alpha);
		}
	}

	public void drawMarkdown(List<String> lines, float x, float y, int color, float yOffset, float height) {
		GlStateManager.enableAlphaTest();

		if (lines == null || lines.isEmpty()) {
			return;
		} else {
			if ((color & 0xFC000000) == 0) {
				color |= 0xFF000000;
			}

			drawMarkdownInner(lines, x, y, color, yOffset, height);
		}
	}

	public void drawMarkdownInner(List<String> lines, float x, final float yIn, int color, float yOffset, float height) {
		final TextRendererExt me = ((TextRendererExt)baseRenderer);
		final boolean rightToLeft = baseRenderer.isRightToLeft();
		final TextureManager textureManager = me.ext_textureManager();
		final FontStorage  fontStorage = me.ext_fontStorage();
		final float baseX = x;
		final float baseRed = ((color >> 16) & 255) / 255.0F;
		final float baseGreen = ((color >> 8) & 255) / 255.0F;
		final float baseBlue = (color & 255) / 255.0F;
		final float red = baseRed;
		final float green = baseGreen;
		final float blue = baseBlue;
		final float alpha = (color >> 24 & 255) / 255.0F;
		final Tessellator tess = Tessellator.getInstance();
		final BufferBuilder buff = tess.getBufferBuilder();
		float y = yIn - yOffset;
		final float yMax = yIn + height;

		Identifier lastGlyphTexture = null;
		buff.begin(7, VertexFormats.POSITION_UV_COLOR);
		int bold = 0;
		int italic = 0;
		int underline = 0;
		int strikethru = 0;
		int indent = 0;
		float margin = 0;
		final int singleLine = 9 + 2;
		final int singleLinePlus = singleLine + (singleLine >> 1);
		int lineHeight = singleLine;
		final float indentWidth = fontStorage.getGlyph(' ').getAdvance() * 4;
		final List<Rectangle> rects = Lists.newArrayList();
		for (String text : lines) {
			if (rightToLeft) {
				text = baseRenderer.mirror(text);
			}

			for(int i = 0; i < text.length(); ++i) {
				final char c = text.charAt(i);

				if (c == ESC && i + 1 < text.length()) {
					switch(text.charAt(++i)) {

					case BOLD:
						++bold;
						break;

					case BOLD_OFF:
						--bold;
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
						++italic;
						break;

					case ITALIC_OFF:
						--italic;
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
						x = baseX;
						break;

					case NEWLINE:
						lineHeight = singleLine;
						break;

					case NEWLINE_PLUS_HALF:
						lineHeight = singleLinePlus;

					default:
						break;
					}
				}  else {
					if (y >= yIn && y + lineHeight <= yMax) {
						final Glyph glyph = fontStorage.getGlyph(c);
						final GlyphRenderer glyphRenderer = fontStorage.getGlyphRenderer(c);
						final Identifier glyphTexture = glyphRenderer.getId();

						if (glyphTexture != null) {
							if (lastGlyphTexture != glyphTexture) {
								tess.draw();
								textureManager.bindTexture(glyphTexture);
								buff.begin(7, VertexFormats.POSITION_UV_COLOR);
								lastGlyphTexture = glyphTexture;
							}

							final float  offset = bold > 0 ? glyph.getBoldOffset() : 0.0F;
							drawGlyph(glyphRenderer, bold > 0, italic > 0, offset, margin + x, y, buff, red, green, blue, alpha);
						}

						final float advance = glyph.getAdvance(bold > 0);

						if (strikethru > 0) {
							rects.add(new Rectangle(margin + x, y + 4.5F, margin + x + advance, y + 4.5F - 1.0F, red, green, blue, alpha));
						}

						if (underline > 0) {
							rects.add(new Rectangle(margin + x, y + 9.0F, margin + x + advance, y + 9.0F - 1.0F, red, green, blue, alpha));
						}

						x += advance;
					}
				}
			}

			y += lineHeight;
			lineHeight = singleLine;
			x = baseX;
		}

		tess.draw();

		if (!rects.isEmpty()) {
			GlStateManager.disableTexture();
			buff.begin(7, VertexFormats.POSITION_COLOR);

			for (final Rectangle r :rects) {
				r.draw(buff);
			}

			tess.draw();
			GlStateManager.enableTexture();
		}
	}

	/**
	 * Returns total vertical height of lines that have already been split to a list.
	 */
	public float verticalHeight(List<String> lines) {
		float y = 0;

		final int singleLine = 9 + 2;
		final int singleLinePlus = singleLine + (singleLine >> 1);
		int lineHeight = singleLine;
		for (final String text : lines) {
			for(int i = 0; i < text.length(); ++i) {
				final char c = text.charAt(i);

				if (c == ESC && i + 1 < text.length()) {
					switch(text.charAt(++i)) {

					case NEWLINE:
						lineHeight = singleLine;
						break;

					case NEWLINE_PLUS_HALF:
						lineHeight = singleLinePlus;

					default:
						break;
					}
				}
			}

			y += lineHeight;
			lineHeight = singleLine;
		}

		return y;
	}

	static class Rectangle {
		protected final float xMin;
		protected final float yMin;
		protected final float xMax;
		protected final float yMax;
		protected final float red;
		protected final float green;
		protected final float blue;
		protected final float alpha;

		private Rectangle(float xMin, float yMin, float xMax, float yMax, float red, float green, float blue, float alpha) {
			this.xMin = xMin;
			this.yMin = yMin;
			this.xMax = xMax;
			this.yMax = yMax;
			this.red = red;
			this.green = green;
			this.blue = blue;
			this.alpha = alpha;
		}

		public void draw(BufferBuilder buffer) {
			buffer.vertex(xMin, yMin, 0.0D).color(red, green, blue, alpha).next();
			buffer.vertex(xMax, yMin, 0.0D).color(red, green, blue, alpha).next();
			buffer.vertex(xMax, yMax, 0.0D).color(red, green, blue, alpha).next();
			buffer.vertex(xMin, yMax, 0.0D).color(red, green, blue, alpha).next();
		}
	}
}
