package com.deeme.tasks.autoshop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.darkbot.api.managers.BackpageAPI;

/**
 * Keeps the item list of the shop in memory. Pages are only read from a
 * background thread, so opening the config or running a tick never waits on the
 * shop.
 */
public final class ShopCatalog {
    private static final String INDEX_PATH = "indexInternal.es";
    private static final Object LOCK = new Object();
    private static final AtomicBoolean REFRESHING = new AtomicBoolean();

    /** Immutable snapshot of the pages read so far, replaced after each read. */
    private static volatile Map<ShopPage, CachedShopPage> pages = Collections.emptyMap();

    private ShopCatalog() {
    }

    /**
     * All the items known right now, ready to be shown in the config dropdown.
     * When some page is missing or outdated a refresh is started in the
     * background, this method never waits for it.
     */
    public static List<ShopItemData> items(BackpageAPI backpage) {
        Map<ShopPage, CachedShopPage> snapshot = pages;

        List<ShopItemData> items = new ArrayList<>();
        for (CachedShopPage cached : snapshot.values()) {
            for (ShopItemData item : cached.getItems()) {
                items.add(new ShopItemData(item));
            }
        }
        items.sort(Comparator.comparing(ShopItemData::getCategory)
                .thenComparing(ShopItemData::getDisplayName));

        if (needsRefresh(snapshot)) {
            requestRefresh(backpage);
        }
        return items;
    }

    /**
     * Item data from the last loaded pages, without making any request.
     */
    public static ShopItemData cached(String itemId) {
        for (CachedShopPage cached : pages.values()) {
            ShopItemData item = cached.find(itemId);
            if (item != null) {
                return new ShopItemData(item);
            }
        }
        return null;
    }

    /**
     * Starts reading the pages that are missing or outdated in a background
     * thread. If a refresh is already running this does nothing.
     */
    public static void requestRefresh(BackpageAPI backpage) {
        if (!needsRefresh(pages) || !backpage.isInstanceValid()
                || !REFRESHING.compareAndSet(false, true)) {
            return;
        }

        // The plugin classloader forbids creating threads directly, the common
        // pool is used instead. It runs in the background and the workers are
        // already daemon threads.
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    refreshStale(backpage);
                } finally {
                    REFRESHING.set(false);
                }
            }).whenComplete((ignored, error) -> {
                if (error != null) {
                    log("The shop list update failed: %s", error.getMessage());
                }
            });
        } catch (RuntimeException e) {
            REFRESHING.set(false);
            log("Could not start the shop list update: %s", e.getMessage());
        }
    }

    /**
     * Shop data of the item from the last read pages. Returns null when the page
     * of the item was read and the item is not offered anymore, so it shouldn't
     * be purchased. When the page hasn't been read the data already known for
     * the item is kept.
     */
    public static ShopItemData resolve(ShopItemData item) {
        if (item == null || item.isEmpty()) {
            return null;
        }

        Map<ShopPage, CachedShopPage> snapshot = pages;
        ShopPage page = item.page != null ? item.page : ShopPage.forCategory(item.category);
        if (page == null) {
            for (ShopPage each : ShopPage.values()) {
                CachedShopPage cached = snapshot.get(each);
                ShopItemData found = cached == null ? null : cached.find(item.itemId);
                if (found != null) {
                    return new ShopItemData(found);
                }
            }
            return new ShopItemData(item);
        }

        CachedShopPage cached = snapshot.get(page);
        if (cached == null || !cached.isLoaded()) {
            return new ShopItemData(item);
        }

        ShopItemData found = cached.find(item.itemId);
        if (found != null) {
            return new ShopItemData(found);
        }

        // The category of the item may have changed, look in the other pages
        for (ShopPage each : ShopPage.values()) {
            if (each == page) {
                continue;
            }
            CachedShopPage other = snapshot.get(each);
            found = other == null ? null : other.find(item.itemId);
            if (found != null) {
                return new ShopItemData(found);
            }
        }
        return null;
    }

    private static boolean needsRefresh(Map<ShopPage, CachedShopPage> snapshot) {
        for (ShopPage page : ShopPage.values()) {
            CachedShopPage cached = snapshot.get(page);
            if (cached == null || !cached.isFresh()) {
                return true;
            }
        }
        return false;
    }

    private static void refreshStale(BackpageAPI backpage) {
        for (ShopPage page : ShopPage.values()) {
            CachedShopPage cached = pages.get(page);
            if (cached != null && cached.isFresh()) {
                continue;
            }

            load(backpage, page);
            if (!backpage.isInstanceValid()) {
                return;
            }
        }
    }

    private static void load(BackpageAPI backpage, ShopPage page) {
        List<ShopItemData> items = null;

        if (backpage.isInstanceValid()) {
            try {
                String html = backpage.getHttp(INDEX_PATH, 3000)
                        .setRawParam("action", "internalDock")
                        .setRawParam("tpl", page.getTpl())
                        .setRawHeader("Referer", backpage.getInstanceURI() + INDEX_PATH + "?action=internalStart")
                        .getContent();
                items = ShopPageParser.parse(html, page);
                if (items == null) {
                    log("Could not read the item list of the %s shop page.", page.getName());
                }
            } catch (IOException | RuntimeException e) {
                log("Could not load the %s shop page: %s", page.getName(), e.getMessage());
            }
        }

        synchronized (LOCK) {
            Map<ShopPage, CachedShopPage> updated = new EnumMap<>(ShopPage.class);
            updated.putAll(pages);
            updated.put(page, items == null
                    ? CachedShopPage.failed(page, pages.get(page))
                    : new CachedShopPage(page, items, System.currentTimeMillis(), true));
            pages = Collections.unmodifiableMap(updated);
        }
    }

    static void log(String format, Object... args) {
        System.out.println("AutoShop: " + String.format(format, args));
    }
}
