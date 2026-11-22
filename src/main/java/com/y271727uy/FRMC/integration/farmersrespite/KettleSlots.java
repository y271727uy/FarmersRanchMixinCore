package com.y271727uy.FRMC.integration.farmersrespite;

public final class KettleSlots {
    public static final int INPUT_COUNT = 4;
    public static final int ORIGINAL_INVENTORY_SIZE = 5;
    public static final int INVENTORY_SIZE = 7;
    public static final int MENU_CONTAINER = INPUT_COUNT;
    public static final int MENU_RESULT = MENU_CONTAINER + 1;
    public static final int PLAYER_START = MENU_RESULT + 1;

    private KettleSlots() {
    }

    public static int inventoryInput(int index) {
        // Keep the drink display, container and result at their original saved indices.
        return index < 2 ? index : ORIGINAL_INVENTORY_SIZE + index - 2;
    }

    public static boolean isAdditionalInput(int slot) {
        return slot >= ORIGINAL_INVENTORY_SIZE && slot < INVENTORY_SIZE;
    }

    public static int inputColumn(int index) {
        return index / 2;
    }

    public static int inputRow(int index) {
        return index % 2;
    }
}
