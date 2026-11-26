package com.github.TavstalDev.rebus.gui;

import com.cryptomorin.xseries.XMaterial;
import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.models.Chest;
import com.github.TavstalDev.rebus.models.Prize;
import com.github.TavstalDev.rebus.models.PrizeData;
import com.github.TavstalDev.rebus.services.box.IBoxService;
import com.github.TavstalDev.rebus.services.prize.IPrizeService;
import com.github.TavstalDev.yggraLib.api.localization.ITranslationService;
import com.github.TavstalDev.yggraLib.api.utils.ListUtil;
import com.github.TavstalDev.yggraLib.api.utils.MapUtil;
import com.github.TavstalDev.yggraLib.bukkit.gui.GuiBase;
import com.github.TavstalDev.yggraLib.bukkit.gui.GuiButton;
import com.github.TavstalDev.yggraLib.bukkit.gui.IGuiService;
import com.github.TavstalDev.yggraLib.bukkit.items.IItemStackAccessor;
import com.github.TavstalDev.yggraLib.bukkit.items.INbtAccessor;
import com.github.TavstalDev.yggraLib.bukkit.utils.AdventureUtils;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * GUI that displays a paginated preview of a chest's prizes with drop chances.
 */
public class PreviewGUI extends GuiBase {
    private final ITranslationService translator;
    private final IItemStackAccessor itemStackAccessor;
    private final INbtAccessor nbtAccessor;
    private final IGuiService gui;
    private final IBoxService boxService;
    private final IPrizeService prizeService;
    public static String ID = "preview";

    private static final HashMap<UUID, Chest> playerChests = new HashMap<>();
    private static final HashMap<UUID, Integer> playerPages = new HashMap<>();

    public PreviewGUI(Rebus plugin, ITranslationService translator, IItemStackAccessor itemStackAccessor,
                      INbtAccessor nbtAccessor, IGuiService guiService, IPrizeService prizeService,
                      IBoxService boxService) {
        super(ID, plugin, "preview.yml",
                "gui.preview.title",
                true,
                6,
                new LinkedHashMap<String, List<String>>() {{
                    put("item_slots", new ArrayList<String>() {{
                        add("10-16");
                        add("19-25");
                        add("28-34");
                        add("37-43");
                    }});
                }},
                new LinkedHashSet<GuiButton>() {{
                    // Placeholder
                    add(new GuiButton(null,"§r ", false, null, false,
                            ListUtil.of("0-9", "17-18", "26-27", "35-36", "44", "46-47", "51-53"),
                            XMaterial.BLACK_STAINED_GLASS_PANE.parseItem()));
                    // Back button
                    add(new GuiButton("BACK", "gui.common.back", true, null, false, ListUtil.of("45"),
                            XMaterial.SPRUCE_DOOR.parseItem()));
                    // Previous button
                    add(new GuiButton("PREVIOUS_PAGE","gui.common.previous-page",
                            true, null, false, ListUtil.of("48"),
                            XMaterial.ARROW.parseItem()));
                    // Current Page button
                    add(new GuiButton("CURRENT_PAGE","gui.common.current-page",
                            true, null, false, ListUtil.of("49"),
                            XMaterial.PAPER.parseItem()));
                    // Next Page button
                    add(new GuiButton("NEXT_PAGE", "gui.common.next-page",
                            true, null, false, ListUtil.of("50"),
                            XMaterial.ARROW.parseItem()));
                }});
        this.translator = translator;
        this.itemStackAccessor = itemStackAccessor;
        this.nbtAccessor = nbtAccessor;
        this.gui = guiService;
        this.prizeService = prizeService;
        this.boxService = boxService;
    }

    /**
     * Refreshes the GUI with the current page's prizes and their drop chances.
     */
    @Override
    public void refresh(@NotNull Player player, @NotNull BaseGui baseGui) {
        UUID playerId = player.getUniqueId();
        Chest chest = playerChests.get(playerId);
        if (chest == null)
            return;

        // 1. Find page button
        int currentPage = playerPages.getOrDefault(playerId, 1);
        Component pageComp = AdventureUtils.parse(translator.localize(playerId,  "gui.common.current-page", MapUtil.of(
                "page", currentPage
        )));
        for (GuiButton btn : buttons) {
            if (Objects.equals(btn.getAction(), "CURRENT_PAGE")) {
                for (Integer slot : btn.getSlots()) {
                    GuiItem guiItem = baseGui.getGuiItem(slot);
                    if (guiItem == null)
                        continue;

                    ItemStack icon = guiItem.getItemStack();
                    itemStackAccessor.setDisplayName(icon, pageComp);
                }
            }
        }

        // Handle dynamic slots
        List<Integer> dynamicSlots = this.dynamicSlots.getOrDefault("item_slots", new ArrayList<>());
        List<Prize> prizes = new ArrayList<>(chest.getPrizes());
        int prizesSize = prizes.size();
        double totalWeight = chest.getTotalWeight();

        for (int i = 0; i < dynamicSlots.size(); i++) {
            int slot = dynamicSlots.get(i);

            if (i >= prizesSize) {
                baseGui.removeItem(slot);
                continue;
            }

            Prize prize = prizes.get(i);
            PrizeData prizeData = prizeService.get(prize.getKey());
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
                lore = itemStackAccessor.getLoreComp(item);
            else
                lore = new ArrayList<>();
            lore.add(Component.text(""));



            String chanceText = translator.localize(playerId, "gui.preview.chance", MapUtil.of("chance", String.format("%.2f", chance * 100)));
            lore.add(AdventureUtils.parse(chanceText, true));
            itemStackAccessor.setLoreComp(item, lore);
            // TODO: Replace with DupeService
            nbtAccessor.set(item, "YGGRA_DUPE", true);

            baseGui.setItem(slot, new GuiItem(item));
        }
        baseGui.open(player);
    }

    /**
     * Handles button actions for navigation (next page, previous page, back, close).
     */
    @Override
    public void executeCommand(Player player, String action, InventoryClickEvent event) {
        UUID playerId = player.getUniqueId();
        switch (action.toUpperCase()) {
            case "NEXT_PAGE": {
                Chest chest = playerChests.get(playerId);
                int arraySize = dynamicSlots.getOrDefault("item_slots", new ArrayList<>()).size();
                if (arraySize == 0) // prevent divide with zero
                    arraySize = 1;
                int maxPage = 1 + (chest.getPrizes().size() / arraySize);
                int page = playerPages.getOrDefault(playerId, 1) + 1;
                if (page > maxPage)
                    break;
                playerPages.put(playerId, page);

                BaseGui menu = gui.getMenu(player, ID);
                if (menu == null)
                    break;
                refresh(player, menu);
                break;
            }
            case "PREVIOUS_PAGE": {
                int page = playerPages.getOrDefault(playerId, 1) - 1;
                if (page <= 0)
                    break;
                playerPages.put(playerId, page);

                BaseGui menu = gui.getMenu(player, ID);
                if (menu == null)
                    break;
                refresh(player, menu);
                break;
            }
            case "CLOSE": {
                gui.close(player, false);
                break;
            }
            case "BACK": {
                gui.close(player, true);
                gui.open(player, MainGUI.ID);
                break;
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
        Chest chest = boxService.getChest(chestId);
        if (chest == null)
            return;

        BaseGui menu = gui.getMenu(player, ID);
        if (menu == null)
            return;

        playerChests.put(player.getUniqueId(), chest);
        playerPages.put(player.getUniqueId(), 1);

        if (isTitleTranslated) {
            menu.updateTitle(AdventureUtils.parse(translator.localize(
                    player.getUniqueId(), menuTitle,
                    MapUtil.of("chest", translator.localize(player.getUniqueId(), chest.getNameKey()))
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