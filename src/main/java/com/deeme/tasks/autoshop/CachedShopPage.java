package com.deeme.tasks.autoshop;

import java.util.Collections;
import java.util.List;

/**
 * The items read from one shop page and when they were read, so the page is not
 * requested on every tick.
 */
public class CachedShopPage {
    private static final long CACHE_MS = 5 * 60_000L;
    private static final long ERROR_CACHE_MS = 60_000L;

    private final ShopPage page;
    private final List<ShopItemData> items;
    private final long fetchedAt;
    private final boolean loaded;

    public CachedShopPage(ShopPage page, List<ShopItemData> items, long fetchedAt, boolean loaded) {
        this.page = page;
        this.items = items == null ? Collections.emptyList() : items;
        this.fetchedAt = fetchedAt;
        this.loaded = loaded;
    }

    /**
     * Keeps the last known items when a new request fails, so the item list is
     * still usable while it is retried sooner.
     */
    public static CachedShopPage failed(ShopPage page, CachedShopPage previous) {
        List<ShopItemData> items = previous == null ? null : previous.getItems();
        return new CachedShopPage(page, items, System.currentTimeMillis(), false);
    }

    public ShopPage getPage() {
        return page;
    }

    public List<ShopItemData> getItems() {
        return items;
    }

    public long getFetchedAt() {
        return fetchedAt;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isFresh() {
        long maxAge = loaded ? CACHE_MS : ERROR_CACHE_MS;
        return System.currentTimeMillis() - fetchedAt < maxAge;
    }

    public ShopItemData find(String itemId) {
        if (itemId == null) {
            return null;
        }
        for (ShopItemData item : items) {
            if (itemId.equals(item.itemId)) {
                return item;
            }
        }
        return null;
    }
}
