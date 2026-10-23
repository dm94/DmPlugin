package com.deeme.tasks.autoshop;

/**
 * A purchase waiting in the queue. The order is sent in batches of the maximum
 * amount the shop allows per request, taken as it's processed, so a large
 * quantity never fills the queue.
 */
public class PurchaseTask {
    final BuyItemConfig config;
    final ShopItemData data;
    int remaining;

    public PurchaseTask(BuyItemConfig config, ShopItemData data, int amount) {
        this.config = config;
        this.data = data;
        this.remaining = amount;
    }

    /**
     * Takes the amount of the next request, reducing what's left of the order.
     */
    public int takeBatch() {
        int batch = data.maxAmount > 0 ? Math.min(remaining, data.maxAmount) : remaining;
        this.remaining -= batch;
        return batch;
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

    public int getRemaining() {
        return remaining;
    }
}
