/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package grondag.mcmd;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.fermion.gui.GuiUtil;
import grondag.fermion.gui.ScreenRenderContext;
import grondag.fermion.gui.control.AbstractControl;
import grondag.fermion.gui.control.Slider;
import grondag.mcmd.node.Node;
import grondag.mcmd.renderer.mc.McMdContentRenderer;

@Environment(EnvType.CLIENT)
public class MarkdownControl extends AbstractControl<MarkdownControl> {
	protected List<String> lines = null;
	final McMdContentRenderer renderer = McMdContentRenderer.builder().build();
	protected float textHeight = 0;

	protected float renderStart = 0;
	protected float buttonHeight = 0;
	protected float buttonOffset = 0;
	protected float maxRenderStart = 0;
	protected float maxButtonOffset = 0;

	protected Slider slider;
	Node markdown;
	final McMdRenderer mcmd;
	public MarkdownControl(ScreenRenderContext renderContext, Node document, Identifier baseFont) {
		super(renderContext);
		markdown = document;
		isDirty = true;
		mcmd = new McMdRenderer(new McMdStyle(), baseFont);
	}


	@Override
	protected void drawContent(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		final VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
		mcmd.drawMarkdown(AffineTransformation.identity().getMatrix(), immediate, lines, left, top, 0, renderStart, height, mouseY);
		immediate.draw();
		drawScrollIfNeeded();
	}

	protected void drawScrollIfNeeded() {
		if (buttonHeight != 0) {
			GuiUtil.drawRect(left + width - theme.scrollbarWidth, top, right, bottom, 0xFF505050);

			GuiUtil.drawRect(left + width - theme.scrollbarWidth + 1, top + 1 + buttonOffset, right - 1, top + buttonOffset + 1 + buttonHeight, 0xFF508080);
		}
	}

	protected void parse(McMdRenderer mcmd, int width) {
		isDirty = false;

		final String text = renderer.render(markdown);

		if (lines == null) {
			lines = new ArrayList<>();
		} else {
			lines.clear();
		}

		mcmd.wrapMarkdownToWidth(text, width, lines);
	}

	@Override
	protected void handleCoordinateUpdate() {
		parse(mcmd, (int) width - theme.scrollbarWidth - theme.internalMargin);

		textHeight = mcmd.verticalHeight(lines);
		buttonHeight = textHeight > height ? (height - 2) * height / textHeight : 0;
		maxRenderStart = textHeight - height;
		maxButtonOffset = buttonHeight == 0 ? 0 : height - 2 - buttonHeight;
		buttonOffset = 0;
		renderStart = 0;
	}

	@Override
	public void drawToolTip(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
	}

	@Override
	protected void handleMouseClick(double mouseX, double mouseY, int clickedMouseButton) {

	}

	@Override
	protected void handleMouseDrag(double mouseX, double mouseY, int clickedMouseButton, double dx, double dy) {

	}

	@Override
	protected void handleMouseScroll(double mouseX, double mouseY, double scrollDelta) {
		if (buttonHeight != 0) {
			buttonOffset -= scrollDelta;
			clamp();
		}
	}

	protected void clamp() {
		if (maxButtonOffset != 0) {
			buttonOffset = MathHelper.clamp(buttonOffset, 0, maxButtonOffset);
			renderStart = maxRenderStart * buttonOffset / maxButtonOffset;
		}
	}
}
