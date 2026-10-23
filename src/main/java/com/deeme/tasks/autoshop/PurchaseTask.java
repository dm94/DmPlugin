package com.deeme.tasks.autoshop;

/**
 * A single purchase waiting in the queue.
 */
public class PurchaseTask {
    final ShopItemData data;
    final int amount;

    public PurchaseTask(ShopItemData data, int amount) {
        this.data = data;
        this.amount = amount;
    }

    public ShopItemData getData() {
        return data;
    }

    public int getAmount() {
        return amount;
    }
}
