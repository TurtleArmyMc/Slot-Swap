package com.turtlearmymc.slotswap.mixin;

import com.turtlearmymc.slotswap.SwapManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
	@Shadow @Final public PlayerEntity player;

	@Inject(at = @At(value = "HEAD"), method = "scrollInHotbar", cancellable = true)
	private void scrollInHotbar(double scrollAmount, CallbackInfo ci) {
		if (SwapManager.isSelectKeyDown() && !player.isUsingItem()) {
			PlayerInventory inv = (PlayerInventory) (Object) this;
			int scrollDelta = (int) Math.signum(scrollAmount);
			SwapManager.scroll(inv, scrollDelta);
			ci.cancel();
		}
	}
}
