package grondag.mcmd.renderer.mc;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public interface McMdRenderer {
    char ESC = '§';

    char OBFUSCATE = 'o';
    char BOLD = 'b';
    char STRIKETHROUGH = 's';
    char UNDERLINE = 'u';
    char ITALIC = 'i';
    char INDENT_PLUS = 't';

    char OBFUSCATE_OFF = 'O';
    char BOLD_OFF = 'B';
    char STRIKETHROUGH_OFF = 'S';
    char UNDERLINE_OFF = 'U';
    char ITALIC_OFF = 'I';
    char INDENT_MINUS = 'T';

    char NEWLINE  = 'n';
    char HALF_NEWLINE  = 'h';

    /** No closing tag - resets x coordinate to current indentation level. */
    char ALIGN_TO_INDENT = 'a';

    String ESC_STR = Character.toString(ESC);

    String ESC_BOLD = ESC_STR + Character.toString(BOLD);
    String ESC_STRIKETHROUGH = ESC_STR + Character.toString(STRIKETHROUGH);
    String ESC_UNDERLINE = ESC_STR + Character.toString(UNDERLINE);
    String ESC_ITALIC = ESC_STR + Character.toString(ITALIC);
    String ESC_OBFUSCATE = ESC_STR + Character.toString(OBFUSCATE);
    String ESC_INDENT_PLUS = ESC_STR + Character.toString(INDENT_PLUS);

    String ESC_BOLD_OFF = ESC_STR + Character.toString(BOLD_OFF);
    String ESC_STRIKETHROUGH_OFF = ESC_STR + Character.toString(STRIKETHROUGH_OFF);
    String ESC_UNDERLINE_OFF = ESC_STR + Character.toString(UNDERLINE_OFF);
    String ESC_ITALIC_OFF = ESC_STR + Character.toString(ITALIC_OFF);
    String ESC_OBFUSCATE_OFF = ESC_STR + Character.toString(OBFUSCATE_OFF);
    String ESC_INDENT_MINUS = ESC_STR + Character.toString(INDENT_MINUS);

    String ESC_ALIGN_TO_INDENT = ESC_STR + Character.toString(ALIGN_TO_INDENT);
    String ESC_NEWLINE = ESC_STR + Character.toString(NEWLINE);
    String ESC_HALF_NEWLINE = ESC_STR + Character.toString(HALF_NEWLINE);


    String wrapMarkdownToWidth(String markdown, int width);

    boolean mcmd_rightToLeft();
    FontStorage mcmd_fontStorage();
    TextureManager mcmd_textureManager();
    void mcmd_drawGlyph(GlyphRenderer glyphRenderer_1, boolean boolean_1, boolean boolean_2, float float_1, float float_2, float float_3, BufferBuilder bufferBuilder_1, float float_4, float float_5, float float_6, float float_7);

    default int mcmd_characterCountForWidth(String text, int width) {
        @SuppressWarnings("resource")
        final TextRenderer me = (TextRenderer)this;
        width = Math.max(1, width);
        final int len = text.length();
        float w = 0.0F;
        int i = 0;
        int lastSpace = -1;
        int indent = 0;
        float margin = 0;
        int boldCount = 0;
        final float indentWidth = mcmd_fontStorage().getGlyph(' ').getAdvance() * 5;

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
                    case HALF_NEWLINE:
                        return i;

                    default:
                        break;
                }

                if (c == ' ') {
                    lastSpace = i;
                }

                if (w != 0.0F) {
                    singleOrEmpty = false;
                }

                w += me.getCharWidth(c);

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
        }

        return i != len && lastSpace != -1 && lastSpace < i ? lastSpace : i;
    }

    default List<String> wrapMarkdownToWidthAsList(String markdown, int width, @Nullable List<String> target) {
        if (target == null)  {
            target = new ArrayList<>();
        }

        String line;
        for(; !markdown.isEmpty(); target.add(line)) {
            final int lineWidth = mcmd_characterCountForWidth(markdown, width);

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

    default float drawMarkdown(List<String> lines, float x, float y, int color) {
        GlStateManager.enableAlphaTest();

        if (lines == null || lines.isEmpty()) {
            return 0;
        } else {

            if ((color & -67108864) == 0) {
                color |= -16777216;
            }

            return drawMarkdownInner(lines, x, y, color);
        }
    }

    default float drawMarkdownInner(List<String> lines, float x, float y, int color) {
        @SuppressWarnings("resource")
        final TextRenderer me = ((TextRenderer)this);
        final boolean rightToLeft = mcmd_rightToLeft();
        final TextureManager textureManager = mcmd_textureManager();
        final FontStorage  fontStorage = mcmd_fontStorage();
        final float baseX = x;
        final float baseRed = (color >> 16 & 255) / 255.0F;
        final float baseGreen = (color >> 8 & 255) / 255.0F;
        final float baseBlue = (color & 255) / 255.0F;
        final float red = baseRed;
        final float green = baseGreen;
        final float blue = baseBlue;
        final float alpha = (color >> 24 & 255) / 255.0F;
        final Tessellator tess = Tessellator.getInstance();
        final BufferBuilder buff = tess.getBufferBuilder();
        Identifier lastGlyphTexture = null;
        buff.begin(7, VertexFormats.POSITION_UV_COLOR);
        int bold = 0;
        int italic = 0;
        int underline = 0;
        int strikethru = 0;
        int indent = 0;
        float margin = 0;
        int lineHeight = me.fontHeight + 2;
        final float indentWidth = fontStorage.getGlyph(' ').getAdvance() * 5;
        final List<Rectangle> rects = Lists.newArrayList();

        for (String text : lines) {
            if (rightToLeft) {
                text = me.mirror(text);
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
                            margin = indent * indentWidth * (me.isRightToLeft() ? -1 : 1);
                            break;

                        case INDENT_MINUS:
                            --indent;
                            margin = indent * indentWidth * (me.isRightToLeft() ? -1 : 1);
                            break;

                        case ALIGN_TO_INDENT:
                            //because margin is always added, resetting to margin is resetting to base
                            x = baseX;
                            break;

                        case NEWLINE:
                            lineHeight = me.fontHeight + 2;
                            break;

                        case HALF_NEWLINE:
                            lineHeight = (me.fontHeight + 2) >> 1;

                default:
                    break;
                    }
                }  else {
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
                        mcmd_drawGlyph(glyphRenderer, bold > 0, italic > 0, offset, margin + x, y, buff, red, green, blue, alpha);
                    }

                    final float advance = glyph.getAdvance(bold > 0);

                    if (strikethru > 0) {
                        rects.add(new Rectangle(margin + x - 1.0F, y + 4.5F, margin + x + advance, y + 4.5F - 1.0F, red, green, blue, alpha));
                    }

                    if (underline > 0) {
                        rects.add(new Rectangle(margin + x - 1.0F, y + 9.0F, margin + x + advance, y + 9.0F - 1.0F, red, green, blue, alpha));
                    }

                    x += advance;
                }
            }

            y += lineHeight;
            lineHeight = me.fontHeight + 2;
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

        return y;
    }

    @Environment(EnvType.CLIENT)
    static class Rectangle {
        protected final float xMin;
        protected final float yMin;
        protected final float xMax;
        protected final float yMax;
        protected final float red;
        protected final float green;
        protected final float blue;
        protected final float alpha;

        private Rectangle(float float_1, float float_2, float float_3, float float_4, float float_5, float float_6, float float_7, float float_8) {
            xMin = float_1;
            yMin = float_2;
            xMax = float_3;
            yMax = float_4;
            red = float_5;
            green = float_6;
            blue = float_7;
            alpha = float_8;
        }

        public void draw(BufferBuilder bufferBuilder_1) {
            bufferBuilder_1.vertex(xMin, yMin, 0.0D).color(red, green, blue, alpha).next();
            bufferBuilder_1.vertex(xMax, yMin, 0.0D).color(red, green, blue, alpha).next();
            bufferBuilder_1.vertex(xMax, yMax, 0.0D).color(red, green, blue, alpha).next();
            bufferBuilder_1.vertex(xMin, yMax, 0.0D).color(red, green, blue, alpha).next();
        }
    }
}
