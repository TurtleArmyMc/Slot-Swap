package com.turtlearmymc.slotswap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public class SwapManager {
	public static final int ROW_COUNT = PlayerInventory.MAIN_SIZE / PlayerInventory.getHotbarSize();
	private static int currentRow;
	private static boolean active;

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

	public static void tryDoSwap(PlayerInventory inv) {
		if (!active) return;
		active = false;
		if (currentRow == 0) return; // Hotbar selected
		final int slot = rowColToSlot(currentRow, inv.selectedSlot);
		MinecraftClient.getInstance().interactionManager.pickFromInventory(slot);
	}
}
