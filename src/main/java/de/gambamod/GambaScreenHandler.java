package de.gambamod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;

/** Server-authoritative 6-row chest menu. Every GUI slot is a button, never an item slot. */
public final class GambaScreenHandler extends GenericContainerScreenHandler {
    public static final int GUI_SIZE = 54;
    private final Inventory machineInventory;

    public GambaScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(GUI_SIZE));
    }

    public GambaScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(GambaMod.GAMBA_HANDLER, syncId, playerInventory, inventory, 6);
        this.machineInventory = inventory;
    }

    public Inventory getMachineInventory() {
        return machineInventory;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Never allow moving GUI decorations or player items while this menu is open.
        // Only normal left-clicks on specific GUI slots activate controls.
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
            return;
        }
        if (actionType != SlotActionType.PICKUP || button != 0) {
            return;
        }
        if (slotIndex < 0 || slotIndex >= GUI_SIZE) {
            return;
        }

        switch (slotIndex) {
            case GambaMachine.BET_DOWN_SLOT -> GambaMachine.changeBet(serverPlayer, machineInventory, -1);
            case GambaMachine.BET_UP_SLOT -> GambaMachine.changeBet(serverPlayer, machineInventory, 1);
            case GambaMachine.SPIN_SLOT -> GambaMachine.startSpin(serverPlayer, machineInventory);
            default -> { /* decorative/information slot */ }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
