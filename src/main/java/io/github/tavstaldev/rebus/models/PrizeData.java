package io.github.tavstaldev.rebus.models;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a prize reward, which can be either a command or an item.
 */
@Getter
public class PrizeData {
    private final EPrizeType type;
    private @Nullable final String command;
    private final ItemStack item;

    /**
     * Creates a command-type prize.
     *
     * @param command The command to execute when the prize is claimed.
     * @param displayItem The item displayed in the GUI for this prize.
     */
    public PrizeData(@NotNull String command, ItemStack displayItem) {
        this.type = EPrizeType.COMMAND;
        this.command = command;
        this.item = displayItem;
    }

    /**
     * Creates an item-type prize.
     *
     * @param itemStack The item stack to give when the prize is claimed.
     */
    public PrizeData(@NotNull ItemStack itemStack) {
        this.type = EPrizeType.ITEM;
        this.command = null;
        this.item = itemStack;
    }
}
