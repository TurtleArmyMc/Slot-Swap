package com.turtlearmymc.slotswap.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.turtlearmymc.slotswap.SwapManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	private static final int HEIGHT = 22;
	private static final int WIDTH = 24;
	@Shadow
	@Final
	static private Identifier WIDGETS_TEXTURE;

	@Shadow
	private int scaledHeight;
	@Shadow
	private int scaledWidth;
	@Shadow
	@Final
	private MinecraftClient client;

	@Invoker("renderHotbarItem")
	public abstract void invokeRenderHotbarItem(
			int x, int y, float tickDelta, PlayerEntity player, ItemStack stack, int seed
	);

	@Inject(method = "render",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions; heldItemTooltips:Z",
					shift = At.Shift.BEFORE))
	public void renderHotbarSwap(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (MinecraftClient.getInstance().currentScreen != null) {
			// If a screen is opened, cancel the swap
			SwapManager.cancel();
			return;
		}

		final PlayerInventory inv = client.player.getInventory();
		if (!Screen.hasAltDown()) {
			SwapManager.tryDoSwap(inv);
			return;
		}

		SwapManager.tryStartSwap();

		// Initialize render system for rendering hotbar
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);

		// Render hotbar selector
		final InGameHud hud = (InGameHud) (Object) this;
		final int xMid = scaledWidth / 2;
		if (SwapManager.getCurrentRow() != 0) {
			final int hotbarX = xMid - 92 + inv.selectedSlot * 20;
			final int hotbarY = scaledHeight - (SwapManager.ROW_COUNT - SwapManager.getCurrentRow() + 1) * HEIGHT - 1;
			hud.drawTexture(matrices, hotbarX, hotbarY, 0, HEIGHT, WIDTH, HEIGHT + 2);
		}

		// Render inventory items. Row 0 is the hotbar
		for (int row = 1; row < SwapManager.ROW_COUNT; row++) {
			// The top rows come first
			final int slotHeight = SwapManager.ROW_COUNT - row;
			final ItemStack slotItem = inv.main.get(SwapManager.rowColToSlot(row, inv.selectedSlot));
			final int itemX = xMid - 88 + inv.selectedSlot * 20;
			final int itemY = scaledHeight - 19 - (slotHeight * HEIGHT);
			invokeRenderHotbarItem(itemX, itemY, tickDelta, client.player, slotItem, row + 2);
		}

		// Return render system settings to what they were before the mixin
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.setShaderTexture(0, DrawableHelper.GUI_ICONS_TEXTURE);
		RenderSystem.disableBlend();
	}

	// Prevent hotbar selector from rendering when a swap is in progress.
	// Simply drawing the selector higher up won't work because it'll be
	// underneath other hud elements, so the selector must be prevented from
	// drawing and be redrawn later instead.
	@Redirect(method = "renderHotbar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture (Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V",
			ordinal = 1))
	public void preventRenderDefaultHotbarSelector(
			InGameHud hud, MatrixStack matrices, int x, int y, int u, int v, int width, int height
	) {
		if (SwapManager.getCurrentRow() == 0) hud.drawTexture(matrices, x, y, u, v, width, height);
	}
}
