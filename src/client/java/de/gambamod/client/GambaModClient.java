package de.gambamod.client;

import de.gambamod.GambaMod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;

public final class GambaModClient implements ClientModInitializer {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onInitializeClient() {
        // GenericContainerScreen is Minecraft's own chest UI. The raw cast is needed because
        // its screen-handler type is the parent GenericContainerScreenHandler, while our
        // registered handler extends that class and adds server-side button behavior.
        HandledScreens.register((ScreenHandlerType) GambaMod.GAMBA_HANDLER,
                (handler, inventory, title) -> new GenericContainerScreen(
                        (GenericContainerScreenHandler) handler,
                        (PlayerInventory) inventory,
                        (Text) title
                ));
    }
}
