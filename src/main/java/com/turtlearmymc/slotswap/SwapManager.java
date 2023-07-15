package com.turtlearmymc.slotswap;

import com.turtlearmymc.slotswap.config.SwapConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class SwapManager implements ClientModInitializer {
	public static final int ROW_COUNT = PlayerInventory.MAIN_SIZE / PlayerInventory.getHotbarSize();
	private static int currentRow;
	private static boolean active;
	public static SwapConfig CONFIG;

	private static @Nullable KeyBinding selectSlotKey = null;

	public static boolean isSelectKeyDown() {
		if (selectSlotKey != null) {
			return selectSlotKey.isPressed();
		} else return false;
	}

	private static List<Integer> getFilledRows(PlayerInventory inv, int col) {
		final List<Integer> filledRows = new ArrayList<>();
		for (int row = 0; row < ROW_COUNT; row++) {
			if (!inv.main.get(rowColToSlot(row, col)).isEmpty()) {
				filledRows.add(row);
			}
		}
		return filledRows;
	}

	public static int rowColToSlot(int row, int col) {
		return PlayerInventory.getHotbarSize() * row + col;
	}

	public static void tryStartSwap() {
		if (!active) {
			active = true;
			currentRow = 0;
		}
	}

	public static void cancel() {
		active = false;
	}

	public static int getCurrentRow() {
		return active ? currentRow : 0;
	}

	private static ClientPlayerInteractionManager getInteractionManager() {
		return MinecraftClient.getInstance().interactionManager;
	}

	public static void scroll(PlayerInventory inv, int rowDelta) {
		tryStartSwap();
		final int col = inv.selectedSlot;
		final List<Integer> rows = getFilledRows(inv, col);
		if (rows.isEmpty()) {
			currentRow = 0;
		} else {
			currentRow = Math.floorMod(currentRow - rowDelta, ROW_COUNT);
			// Only select hotbar or a filled row
			while (currentRow != 0 && !rows.contains(currentRow)) {
				currentRow = Math.floorMod(currentRow - rowDelta, ROW_COUNT);
			}
		}
	}

	private static void swapSlots(PlayerInventory inv, int slotA, int slotB) {

		// clickSlot has a weird indexing:
		// first slot arg is slot index, what is kinda random
		// https://videa.hu/videok/film-animacio/laputa-az-egi-palota-124-perc-1986-KZZvhnBm2KnCxTNx?start=1920.560579

		// second item is normal slot index

		final int slotAIndex = slotA < 9 ? slotA + PlayerInventory.MAIN_SIZE : slotA;
		getInteractionManager().clickSlot(0, slotAIndex, slotB, SlotActionType.SWAP, MinecraftClient.getInstance().player);
	}


	public static void tryDoSwap(PlayerInventory inv) {
		if (!active) return;
		active = false;
		if (currentRow == 0) return; // Hotbar selected
		switch (CONFIG.swapMode) {
			case COMPATIBLE -> {
				final int slot = rowColToSlot(currentRow, inv.selectedSlot);
				getInteractionManager().pickFromInventory(slot);
			}
			case SWAP -> {
				final int slotA = rowColToSlot(currentRow, inv.selectedSlot);
				final int slotB = rowColToSlot(0, inv.selectedSlot);
				swapSlots(inv, slotA, slotB);
			}
			case SHIFT -> {
				final int column = inv.selectedSlot;
				final int distance = currentRow;

				for (int cycleStart = 0, nMoved = 0; nMoved != ROW_COUNT; cycleStart++) {
					int i = cycleStart;
					int displaced = i;
					do {
						i += distance;
						if (i >= ROW_COUNT)
							i -= ROW_COUNT;
						swapSlots(inv, rowColToSlot(displaced, column), rowColToSlot(i, column));
						displaced = i;
						nMoved++;
					} while ((i + distance) % ROW_COUNT != cycleStart);
					nMoved++;
				}
			}
		}
	}

	@Override
	public void onInitializeClient() {

		AutoConfig.register(SwapConfig.class, JanksonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(SwapConfig.class).get();

		selectSlotKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.slotswap.swap",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_LEFT_ALT,
				KeyBinding.INVENTORY_CATEGORY
		));
	}
}
