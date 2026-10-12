package de.gambamod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import com.mojang.brigadier.arguments.IntegerArgumentType;

public final class GambaMod implements ModInitializer {
    public static final String MOD_ID = "gamba_mod";

    public static final ScreenHandlerType<GambaScreenHandler> GAMBA_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MOD_ID, "gamba_slot"),
            new ScreenHandlerType<>(GambaScreenHandler::new, FeatureSet.empty())
    );

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("gamba")
                        .executes(context -> {
                            GambaMachine.open(context.getSource().getPlayer());
                            return 1;
                        })
                        .then(literal("bet")
                                .then(argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            GambaMachine.setBetFromCommand(player, amount);
                                            return 1;
                                        })))
                        .then(literal("balance").executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            context.getSource().sendFeedback(
                                    () -> Text.literal("§bGamba-Guthaben: §a" + GambaMachine.countEmeralds(player) + " Emeralds"),
                                    false
                            );
                            return 1;
                        }))
                        .then(literal("odds").executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            GambaMachine.sendOdds(player);
                            return 1;
                        }))
        ));

        ServerTickEvents.END_SERVER_TICK.register(GambaMachine::tick);
    }
}
