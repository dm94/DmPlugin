package com.deeme.tasks.autoshop;

/**
 * A single purchase waiting in the queue.
 */
public class PurchaseTask {
    final BuyItemConfig config;
    final ShopItemData data;
    final int amount;

    public PurchaseTask(BuyItemConfig config, ShopItemData data, int amount) {
        this.config = config;
        this.data = data;
        this.amount = amount;
    }

    /**
     * The configuration slot that requested the purchase, so its remaining
     * batches can be discarded when the slot is disabled.
     */
    public BuyItemConfig getConfig() {
        return config;
    }

    public ShopItemData getData() {
        return data;
    }

    public int getAmount() {
        return amount;
    }
}
