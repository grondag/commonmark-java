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

import java.util.function.IntSupplier;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import com.mojang.math.Transformation;
import grondag.fermion.gui.GuiUtil;
import grondag.fermion.gui.ScreenRenderContext;
import grondag.fermion.gui.control.AbstractControl;
import grondag.fermion.gui.control.Slider;
import grondag.mcmd.node.Node;
import grondag.mcmd.renderer.mc.McMdContentRenderer;

@Environment(EnvType.CLIENT)
public class MarkdownControl extends AbstractControl<MarkdownControl> {
	protected final Supplier<Node> markdownSupplier;
	protected final IntSupplier versionSupplier;

	protected final ObjectArrayList<String> lines = new ObjectArrayList<>();
	final McMdContentRenderer renderer = McMdContentRenderer.builder().build();
	protected float textHeight = 0;

	protected float renderStart = 0;
	protected float buttonHeight = 0;
	protected float buttonOffset = 0;
	protected float maxRenderStart = 0;
	protected float maxButtonOffset = 0;

	protected Slider slider;
	protected int version;
	final McMdRenderer mcmd;

	public MarkdownControl(ScreenRenderContext renderContext, Supplier<Node> markdownSupplier, IntSupplier versionSupplier, ResourceLocation baseFont) {
		super(renderContext);
		this.markdownSupplier = markdownSupplier;
		this.versionSupplier = versionSupplier;
		version = versionSupplier.getAsInt();
		isDirty = true;
		mcmd = new McMdRenderer(new McMdStyle(), baseFont);
	}

	@Override
	protected void drawContent(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		final int v = versionSupplier.getAsInt();

		if (isDirty || v != version) {
			parse();
			version = v;
		}

		final MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		mcmd.drawMarkdown(Transformation.identity().getMatrix(), immediate, lines, left, top, 0, renderStart, height, mouseY);
		immediate.endBatch();
		drawScrollIfNeeded(matrixStack.last().pose());
	}

	protected void drawScrollIfNeeded(Matrix4f matrix) {
		if (buttonHeight != 0) {
			GuiUtil.drawRect(matrix, left + width - theme.scrollbarWidth, top, right, bottom, 0xFF505050);

			GuiUtil.drawRect(matrix, left + width - theme.scrollbarWidth + 1, top + 1 + buttonOffset, right - 1, top + buttonOffset + 1 + buttonHeight, 0xFF508080);
		}
	}

	protected void parse() {
		isDirty = false;
		final String text = renderer.render(markdownSupplier.get());
		lines.clear();
		final int w = (int) width - theme.scrollbarWidth - theme.internalMargin;
		mcmd.wrapMarkdownToWidth(text, w, lines);
	}

	@Override
	protected void handleCoordinateUpdate() {
		parse();

		textHeight = mcmd.verticalHeight(lines);
		buttonHeight = textHeight > height ? (height - 2) * height / textHeight : 0;
		maxRenderStart = textHeight - height;
		maxButtonOffset = buttonHeight == 0 ? 0 : height - 2 - buttonHeight;
		buttonOffset = 0;
		renderStart = 0;
	}

	@Override
	public void drawToolTip(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
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
			buttonOffset = Mth.clamp(buttonOffset, 0, maxButtonOffset);
			renderStart = maxRenderStart * buttonOffset / maxButtonOffset;
		}
	}
}
