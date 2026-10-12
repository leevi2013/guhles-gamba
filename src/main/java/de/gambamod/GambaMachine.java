package de.gambamod;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GambaMachine {
    public static final int BET_DOWN_SLOT = 38;
    public static final int BET_DISPLAY_SLOT = 40;
    public static final int BET_UP_SLOT = 42;
    public static final int SPIN_SLOT = 49;
    public static final int SYMBOL_1_SLOT = 21;
    public static final int SYMBOL_2_SLOT = 22;
    public static final int SYMBOL_3_SLOT = 23;
    private static final int MIN_BET = 1;
    private static final int MAX_BET = 64;
    private static final int SPIN_MIN_TICKS = 40;
    private static final int SPIN_MAX_TICKS = 80;

    private static final Map<UUID, Integer> BETS = new HashMap<>();
    private static final java.util.Set<UUID> INVALID_BETS = new java.util.HashSet<>();
    private static final Map<UUID, SpinState> ACTIVE_SPINS = new HashMap<>();

    private GambaMachine() { }

    public enum OreSymbol {
        COAL(Items.COAL_ORE, "Coal Ore", 0.5),
        IRON(Items.IRON_ORE, "Iron Ore", 1.2),
        GOLD(Items.GOLD_ORE, "Gold Ore", 2.0),
        DIAMOND(Items.DIAMOND_ORE, "Diamond Ore", 5.0),
        EMERALD(Items.EMERALD_ORE, "Emerald Ore", 10.0),
        NETHERITE(Items.ANCIENT_DEBRIS, "Ancient Debris / Netherite", 25.0);

        public final Item item;
        public final String displayName;
        public final double multiplier;

        OreSymbol(Item item, String displayName, double multiplier) {
            this.item = item;
            this.displayName = displayName;
            this.multiplier = multiplier;
        }

        public static OreSymbol random() {
            OreSymbol[] symbols = values();
            return symbols[ThreadLocalRandom.current().nextInt(symbols.length)];
        }
    }

    private static final class SpinState {
        private final ServerPlayerEntity player;
        private final Inventory inventory;
        private final int bet;
        private final int durationTicks;
        private int elapsedTicks = 0;

        private SpinState(ServerPlayerEntity player, Inventory inventory, int bet, int durationTicks) {
            this.player = player;
            this.inventory = inventory;
            this.bet = bet;
            this.durationTicks = durationTicks;
        }
    }

    public static int getBet(ServerPlayerEntity player) {
        return BETS.getOrDefault(player.getUuid(), 1);
    }

    public static int countEmeralds(ServerPlayerEntity player) {
        int total = 0;
        PlayerInventory inventory = player.getInventory();
        // Main inventory and hotbar only; armor/offhand are not used as currency.
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isOf(Items.EMERALD)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static void setBetFromCommand(ServerPlayerEntity player, int amount) {
        if (!validateBet(player, amount)) {
            if (player.currentScreenHandler instanceof GambaScreenHandler handler) {
                showInvalidBet(handler.getMachineInventory());
            }
            return;
        }
        BETS.put(player.getUuid(), amount);
        INVALID_BETS.remove(player.getUuid());
        if (player.currentScreenHandler instanceof GambaScreenHandler handler) {
            fillGui(handler.getMachineInventory(), amount, ACTIVE_SPINS.containsKey(player.getUuid()));
        }
        player.sendMessage(Text.literal("§bGamba-Einsatz gesetzt: §e" + amount + " Emeralds"), false);
    }

    private static boolean validateBet(ServerPlayerEntity player, int amount) {
        if (amount <= 0) {
            INVALID_BETS.add(player.getUuid());
            player.sendMessage(Text.literal("§cFehler: Der Einsatz muss größer als 0 sein. Drehung blockiert, bis ein gültiger Einsatz gesetzt wird."), false);
            return false;
        }
        if (amount > MAX_BET) {
            INVALID_BETS.add(player.getUuid());
            player.sendMessage(Text.literal("§cFehler: Maximaler Einsatz sind " + MAX_BET + " Emeralds. Drehung blockiert, bis ein gültiger Einsatz gesetzt wird."), false);
            return false;
        }
        INVALID_BETS.remove(player.getUuid());
        return true;
    }

    public static void open(ServerPlayerEntity player) {
        int bet = getBet(player);
        SimpleInventory inventory = new SimpleInventory(GambaScreenHandler.GUI_SIZE);
        fillGui(inventory, bet, false);
        if (INVALID_BETS.contains(player.getUuid())) showInvalidBet(inventory);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, playerEntity) -> new GambaScreenHandler(syncId, playerInventory, inventory),
                Text.literal("§9§lGAMBA SLOT")
        ));
    }

    public static void changeBet(ServerPlayerEntity player, Inventory inventory, int delta) {
        if (ACTIVE_SPINS.containsKey(player.getUuid())) {
            player.sendMessage(Text.literal("§cWährend des Drehens kann der Einsatz nicht geändert werden."), false);
            return;
        }
        int current = getBet(player);
        int next = current + delta;
        if (next < MIN_BET) next = MIN_BET;
        if (next > MAX_BET) next = MAX_BET;
        BETS.put(player.getUuid(), next);
        INVALID_BETS.remove(player.getUuid());
        fillGui(inventory, next, false);
        player.sendMessage(Text.literal("§bEinsatz: §e" + next + " Emeralds"), true);
    }

    public static void startSpin(ServerPlayerEntity player, Inventory inventory) {
        UUID uuid = player.getUuid();
        if (ACTIVE_SPINS.containsKey(uuid)) {
            player.sendMessage(Text.literal("§cDie Slotmaschine dreht bereits!"), false);
            return;
        }

        int bet = getBet(player);
        // A previous invalid amount must be explicitly corrected before a spin can start.
        if (INVALID_BETS.contains(uuid)) {
            player.sendMessage(Text.literal("§cDrehung blockiert: Setze zuerst einen gültigen Einsatz mit /gamba bet 1 bis 64."), false);
            return;
        }
        // A second server-side validation is intentional: no GUI or stale client state can bypass it.
        if (bet <= 0 || !validateBet(player, bet)) {
            return;
        }
        if (countEmeralds(player) < bet) {
            player.sendMessage(Text.literal("§cNicht genug Emeralds. Benötigt: " + bet + "."), false);
            return;
        }
        if (!removeEmeralds(player, bet)) {
            player.sendMessage(Text.literal("§cDer Einsatz konnte nicht abgebucht werden. Drehung blockiert."), false);
            return;
        }

        int duration = ThreadLocalRandom.current().nextInt(SPIN_MIN_TICKS, SPIN_MAX_TICKS + 1);
        SpinState state = new SpinState(player, inventory, bet, duration);
        ACTIVE_SPINS.put(uuid, state);
        inventory.setStack(SPIN_SLOT, named(Items.CLOCK, "§e§lDreht ..."));
        inventory.markDirty();
        player.sendMessage(Text.literal("§bSlotmaschine gestartet! §7Einsatz: §e" + bet + " Emeralds"), false);
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, SpinState>> iterator = ACTIVE_SPINS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SpinState> entry = iterator.next();
            SpinState state = entry.getValue();
            if (state.player.isRemoved()) {
                iterator.remove();
                continue;
            }

            state.elapsedTicks++;
            if (state.elapsedTicks % 3 == 0 && state.elapsedTicks < state.durationTicks) {
                state.inventory.setStack(SYMBOL_1_SLOT, symbolStack(OreSymbol.random()));
                state.inventory.setStack(SYMBOL_2_SLOT, symbolStack(OreSymbol.random()));
                state.inventory.setStack(SYMBOL_3_SLOT, symbolStack(OreSymbol.random()));
                state.inventory.markDirty();
            }

            if (state.elapsedTicks >= state.durationTicks) {
                finishSpin(state);
                iterator.remove();
            }
        }
    }

    private static void finishSpin(SpinState state) {
        OreSymbol first = OreSymbol.random();
        OreSymbol second = OreSymbol.random();
        OreSymbol third = OreSymbol.random();
        state.inventory.setStack(SYMBOL_1_SLOT, symbolStack(first));
        state.inventory.setStack(SYMBOL_2_SLOT, symbolStack(second));
        state.inventory.setStack(SYMBOL_3_SLOT, symbolStack(third));
        state.inventory.setStack(SPIN_SLOT, named(Items.LIME_CONCRETE, "§a§lDREHEN"));
        state.inventory.markDirty();

        if (first == second && second == third) {
            int payout = (int) Math.floor(state.bet * first.multiplier);
            if (payout > 0) {
                giveEmeralds(state.player, payout);
            }
            state.player.sendMessage(Text.literal(
                    "§6§lDREI GLEICHE! §e" + first.displayName + " §7(" + first.multiplier + "x) — Auszahlung: §a" + payout + " Emeralds"
            ), false);
        } else {
            state.player.sendMessage(Text.literal("§cKeine drei gleichen Symbole. Einsatz verloren."), false);
        }
    }

    private static boolean removeEmeralds(ServerPlayerEntity player, int amount) {
        PlayerInventory inventory = player.getInventory();
        if (countEmeralds(player) < amount) return false;
        int remaining = amount;
        for (int slot = 0; slot < 36 && remaining > 0; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isOf(Items.EMERALD)) continue;
            int taken = Math.min(stack.getCount(), remaining);
            stack.decrement(taken);
            remaining -= taken;
        }
        inventory.markDirty();
        return remaining == 0;
    }

    private static void giveEmeralds(ServerPlayerEntity player, int amount) {
        ItemStack reward = new ItemStack(Items.EMERALD, amount);
        player.getInventory().insertStack(reward);
        if (!reward.isEmpty()) {
            player.dropItem(reward, false);
        }
    }

    private static ItemStack symbolStack(OreSymbol symbol) {
        String label = symbol.displayName + " — " + String.format(java.util.Locale.ROOT, "%.1fx", symbol.multiplier);
        if (symbol == OreSymbol.COAL) label += " (Verlust)";
        if (symbol == OreSymbol.NETHERITE) label += " (JACKPOT)";
        return named(symbol.item, "§b" + label);
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return stack;
    }

    public static void fillGui(Inventory inventory, int bet, boolean spinning) {
        for (int i = 0; i < inventory.size(); i++) {
            inventory.setStack(i, named(Items.BLUE_STAINED_GLASS_PANE, " "));
        }
        inventory.setStack(13, named(Items.BOOK, "§b§lSYMBOLWERTE — /gamba odds"));
        inventory.setStack(SYMBOL_1_SLOT, symbolStack(OreSymbol.random()));
        inventory.setStack(SYMBOL_2_SLOT, symbolStack(OreSymbol.random()));
        inventory.setStack(SYMBOL_3_SLOT, symbolStack(OreSymbol.random()));
        inventory.setStack(30, named(Items.PAPER, "§bEinsatz ändern: §f/gamba bet <zahl>"));
        inventory.setStack(BET_DOWN_SLOT, named(Items.REDSTONE_TORCH, "§cEinsatz −1"));
        inventory.setStack(BET_DISPLAY_SLOT, named(Items.EMERALD, "§b§lEinsatz: §e" + bet + " Emeralds"));
        inventory.setStack(BET_UP_SLOT, named(Items.SLIME_BALL, "§aEinsatz +1"));
        inventory.setStack(SPIN_SLOT, named(spinning ? Items.CLOCK : Items.LIME_CONCRETE,
                spinning ? "§e§lDreht ..." : "§a§lDREHEN"));
        inventory.setStack(50, named(Items.BOOK, "§fGewinn nur bei 3 gleichen Symbolen"));
        inventory.markDirty();
    }

    private static void showInvalidBet(Inventory inventory) {
        inventory.setStack(BET_DISPLAY_SLOT, named(Items.BARRIER, "§cUngültiger Einsatz — /gamba bet 1 bis 64"));
        inventory.markDirty();
    }

    public static void sendOdds(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("§b§lGAMBA — SYMBOLWERTE"), false);
        for (OreSymbol symbol : OreSymbol.values()) {
            player.sendMessage(Text.literal("§7• §f" + symbol.displayName + " §8— §e" +
                    String.format(java.util.Locale.ROOT, "%.1fx", symbol.multiplier)), false);
        }
        player.sendMessage(Text.literal("§7Gewinn gibt es nur bei drei identischen Symbolen in der mittleren Reihe. Auszahlung wird auf ganze Emeralds abgerundet."), false);
    }
}
