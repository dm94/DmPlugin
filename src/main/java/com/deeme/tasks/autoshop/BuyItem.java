package com.deeme.tasks.autoshop;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.types.Condition;

@Configuration("buy_item_conditions")
public class BuyItem implements BuyItemConfig {
    public transient long nextCheck = 0;

    @Option("general.enabled")
    public boolean enable = false;

    @Option("general.next_check_time_minutes")
    @Number(min = 1, max = 1440, step = 1)
    public int timeToCheck = 20;

    @Option("buy_item_conditions.quantity")
    @Number(min = 1, step = 1, max = 1000000)
    public int quantity = 1;

    @Option("buy_item_conditions.item")
    @Dropdown(options = ItemSupplier.class)
    public ShopItemData item;

    /**
     * Item id saved by versions that only stored the id of the item. It's only
     * read to convert old configurations, see {@link #migrateLegacyItem()}.
     */
    @Option.Ignore
    public String itemToBuy;

    @Option("general.condition")
    public Condition condition;

    @Option("quantity_condition")
    public QuantityCondition quantityCondition = new QuantityCondition();

    @Override
    public long getNextCheck() {
        return nextCheck;
    }

    @Override
    public void setNextCheck(long nextCheck) {
        this.nextCheck = nextCheck;
    }

    @Override
    public boolean isEnabled() {
        return enable;
    }

    @Override
    public int getTimeToCheck() {
        return timeToCheck;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }

    /**
     * Configurations saved by older versions stored only the id of the item.
     * Converts it into the item selection so they keep buying the same item.
     * The shop list may not have been read yet, in that case the selection is
     * built with just the id and completed once the shop list is available.
     */
    public void migrateLegacyItem() {
        if (itemToBuy == null) {
            return;
        }

        String itemId = itemToBuy.trim();
        // Only used to read old configurations, it should not be saved again
        itemToBuy = null;

        if (itemId.isEmpty() || (item != null && !item.isEmpty())) {
            return;
        }

        ShopItemData legacy = ShopCatalog.cached(itemId);
        if (legacy == null) {
            legacy = new ShopItemData();
            legacy.itemId = itemId;
        }
        item = legacy;
    }

    @Override
    public ShopItem getShopItem() {
        return item;
    }

    @Override
    public Condition getCondition() {
        return condition;
    }

    @Override
    public QuantityCondition getQuantityCondition() {
        return quantityCondition;
    }
}
