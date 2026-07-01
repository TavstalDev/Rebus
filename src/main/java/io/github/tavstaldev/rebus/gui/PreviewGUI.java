package io.github.tavstaldev.rebus.gui;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.managers.ChestManager;
import io.github.tavstaldev.rebus.managers.PrizeManager;
import io.github.tavstaldev.rebus.models.Chest;
import io.github.tavstaldev.yggra.core.gui.GuiBase;
import io.github.tavstaldev.yggra.core.gui.GuiButton;
import io.github.tavstaldev.yggra.core.gui.GuiDupeDetector;
import io.github.tavstaldev.yggra.core.gui.GuiManager;
import io.github.tavstaldev.yggra.core.services.TranslationService;
import io.github.tavstaldev.yggra.items.api.utils.AdventureUtils;
import io.github.tavstaldev.yggra.shadow.triumpGui.guis.BaseGui;
import io.github.tavstaldev.yggra.shadow.triumpGui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * GUI that displays a paginated preview of a chest's prizes with drop chances.
 */
public class PreviewGUI extends GuiBase {
    private final TranslationService translator;
    private final GuiManager gui;
    private final ChestManager chestManager;
    private final PrizeManager prizeManager;
    public static String ID = "preview";

    private static final HashMap<UUID, Chest> playerChests = new HashMap<>();
    private static final HashMap<UUID, Integer> playerPages = new HashMap<>();

    /**
     * Creates the preview GUI.
     *
     * @param plugin       The plugin instance.
     * @param prizeManager The prize manager for looking up prize data.
     * @param chestManager The chest manager for looking up chests.
     */
    public PreviewGUI(Rebus plugin, PrizeManager prizeManager, ChestManager chestManager) {
        super(ID, plugin, "preview.yml",
                "gui.preview.title",
                true,
                6,
                new LinkedHashMap<>() {{
                    put("item_slots", new ArrayList<>() {{
                        add("10-16");
                        add("19-25");
                        add("28-34");
                        add("37-43");
                    }});
                }},
                new LinkedHashSet<>() {{
                    // Placeholder
                    add(new GuiButton(null,"§r", false, null, false,
                            List.of("0-9", "17-18", "26-27", "35-36", "44", "46-47", "51-53"),
                            new ItemStack(Material.BLACK_STAINED_GLASS_PANE)));
                    // Back button
                    add(new GuiButton("BACK", "gui.common.back", true, null, false, List.of("45"),
                            new ItemStack(Material.SPRUCE_DOOR)));
                    // Previous button
                    add(new GuiButton("PREVIOUS_PAGE","gui.common.previous-page",
                            true, null, false, List.of("48"),
                            new ItemStack(Material.ARROW)));
                    // Current Page button
                    add(new GuiButton("CURRENT_PAGE","gui.common.current-page",
                            true, null, false, List.of("49"),
                            new ItemStack(Material.PAPER)));
                    // Next Page button
                    add(new GuiButton("NEXT_PAGE", "gui.common.next-page",
                            true, null, false, List.of("50"),
                            new ItemStack(Material.ARROW)));
                }});
        this.translator = plugin.translator();
        this.gui = plugin.gui();
        this.prizeManager = prizeManager;
        this.chestManager = chestManager;
    }

    /**
     * Refreshes the GUI with the current page's prizes and their drop chances.
     */
    @Override
    public void refresh(@NotNull Player player, @NotNull BaseGui baseGui) {
        UUID playerId = player.getUniqueId();
        var chest = playerChests.get(playerId);
        if (chest == null)
            return;

        // 1. Find page button
        int currentPage = playerPages.getOrDefault(playerId, 1);
        for (var btn : buttons) {
            if (Objects.equals(btn.getAction(), "CURRENT_PAGE")) {
                if (btn.getTitle() == null)
                    continue;

                String pageText = btn.shouldTranslateTitle() ? translator.localize(player, btn.getTitle(), Map.of(
                        "page", String.valueOf(currentPage)
                )) : btn.getTitle();
                if (pageText == null)
                    continue;
                Component pageComp = AdventureUtils.parse(pageText, true);

                for (Integer slot : btn.getSlots()) {
                    var sgBtn = baseGui.getGuiItem(slot);
                    if (sgBtn == null)
                        continue;

                    ItemStack icon = sgBtn.getItemStack();
                    icon.editMeta(x -> {
                       x.displayName(pageComp);
                    });
                }
            }
        }

        // Handle dynamic slots
        List<Integer> dynamicSlots = this.dynamicSlots.getOrDefault("item_slots", new ArrayList<>());
        var prizes = chest.getPrizes().stream().toList();
        int prizesSize = prizes.size();
        double totalWeight = chest.getTotalWeight();

        for (int i = 0; i < dynamicSlots.size(); i++) {
            int slot = dynamicSlots.get(i);

            if (i >= prizesSize) {
                baseGui.removeItem(slot);
                continue;
            }

            var prize = prizes.get(i);
            var prizeData = prizeManager.get(prize.getKey());
            if (prizeData == null) { // configuration error
                logger.error("Failed to get prizeData for: " + prize.getKey());
                baseGui.removeItem(slot);
                continue;
            }

            ItemStack item = prizeData.getItem();
            item.setAmount(prize.getMinAmount());
            double chance = (double)prize.getWeight() / totalWeight;

            ItemMeta meta = item.getItemMeta();
            List<Component> lore;
            if (meta.hasLore())
                lore = new ArrayList<>(Objects.requireNonNull(meta.lore()));
            else
                lore = new ArrayList<>();
            lore.add(Component.text(""));
            String chanceText = translator.localize(player, "gui.preview.chance", Map.of("chance", String.format("%.2f", chance * 100)));
            lore.add(AdventureUtils.parse(chanceText, true));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(GuiDupeDetector.getDupeProtectedKey(), PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
            baseGui.setItem(slot, new GuiItem(item));
        }
        baseGui.open(player);
    }

    /**
     * Handles button actions for navigation (next page, previous page, back, close).
     */
    @Override
    public void executeCommand(Player player, String action, InventoryClickEvent event) {
        var playerId = player.getUniqueId();
        switch (action.toUpperCase()) {
            case "NEXT_PAGE" -> {
                var chest = playerChests.get(playerId);
                int arraySize = dynamicSlots.getOrDefault("item_slots", new ArrayList<>()).size();
                if (arraySize == 0) // prevent divide with zero
                    arraySize = 1;
                int maxPage = 1 + (chest.getPrizes().size() / arraySize);
                int page = playerPages.getOrDefault(playerId, 1) + 1;
                if (page > maxPage)
                    return;
                playerPages.put(playerId, page);

                BaseGui menu = gui.getMenu(player, ID);
                if (menu == null)
                    break;
                refresh(player, menu);
            }
            case "PREVIOUS_PAGE" -> {
                int page = playerPages.getOrDefault(playerId, 1) - 1;
                if (page <= 0)
                    return;
                playerPages.put(playerId, page);

                BaseGui menu = gui.getMenu(player, ID);
                if (menu == null)
                    break;
                refresh(player, menu);
            }
            case "CLOSE" -> {
                gui.close(player, false);
            }
            case "BACK" -> {
                gui.close(player, true);
                gui.open(player, MainGUI.ID);
            }
        }
    }

    /**
     * Opens the preview GUI for a specific chest.
     *
     * @param args Expects the chest ID as the first argument.
     */
    @Override
    public void onOpen(@NotNull Player player, Object... args) {
        if (args.length == 0)
            return;

        String chestId = (String) args[0];
        var chest = chestManager.getChest(chestId);
        if (chest == null)
            return;

        BaseGui menu = gui.getMenu(player, ID);
        if (menu == null)
            return;

        playerChests.put(player.getUniqueId(), chest);
        playerPages.put(player.getUniqueId(), 1);

        if (isTitleTranslated) {
            menu.updateTitle(AdventureUtils.parse(translator.localize(
                    player, menuTitle,
                    Map.of("chest", translator.localize(player, chest.getNameKey()))
            ), true));
        }
        refresh(player, menu);
    }

    /**
     * Cleans up cached data when the player closes the GUI.
     */
    @Override
    public void onClose(@NotNull Player player) {
        playerPages.remove(player.getUniqueId());
        playerChests.remove(player.getUniqueId());
    }
}