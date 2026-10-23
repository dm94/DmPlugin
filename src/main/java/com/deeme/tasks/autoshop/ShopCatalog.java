package com.deeme.tasks.autoshop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.darkbot.api.managers.BackpageAPI;

/**
 * Keeps the item list of the shop in memory. Pages are only read from a
 * background thread, so opening the config or running a tick never waits on the
 * shop.
 */
public final class ShopCatalog {
    private static final String INDEX_PATH = "indexInternal.es";

    /**
     * One catalog per account and game instance. The daily limits are account
     * specific, so the data of one session can't be used for another.
     */
    private static final Map<String, SessionState> SESSIONS = new ConcurrentHashMap<>();

    private ShopCatalog() {
    }

    private static final class SessionState {
        private final AtomicBoolean refreshing = new AtomicBoolean();
        /** Immutable snapshot of the pages read so far, replaced after each read. */
        private volatile Map<ShopPage, CachedShopPage> pages = Collections.emptyMap();
    }

    /**
     * All the items known right now, ready to be shown in the config dropdown.
     * When some page is missing or outdated a refresh is started in the
     * background, this method never waits for it.
     */
    public static List<ShopItemData> items(BackpageAPI backpage) {
        Map<ShopPage, CachedShopPage> snapshot = pagesOf(backpage);

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
     * Item data from the last loaded pages of the session, without making any
     * request.
     */
    public static ShopItemData cached(BackpageAPI backpage, String itemId) {
        for (CachedShopPage cached : pagesOf(backpage).values()) {
            ShopItemData item = cached.find(itemId);
            if (item != null) {
                return new ShopItemData(item);
            }
        }
        return null;
    }

    /**
     * Starts reading the pages that are missing or outdated in a background
     * thread. If a refresh is already running for the session this does nothing.
     */
    public static void requestRefresh(BackpageAPI backpage) {
        String sessionKey = sessionKey(backpage);
        if (sessionKey == null) {
            return;
        }

        SessionState session = state(sessionKey);
        if (!needsRefresh(session.pages) || !session.refreshing.compareAndSet(false, true)) {
            return;
        }

        // The plugin classloader forbids creating threads directly, the common
        // pool is used instead. It runs in the background and the workers are
        // already daemon threads.
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    refreshStale(backpage, session, sessionKey);
                } finally {
                    session.refreshing.set(false);
                }
            }).whenComplete((ignored, error) -> {
                if (error != null) {
                    log("The shop list update failed: %s", error.getMessage());
                }
            });
        } catch (RuntimeException e) {
            session.refreshing.set(false);
            log("Could not start the shop list update: %s", e.getMessage());
        }
    }

    /**
     * Shop data of the item from the last read pages of the session. Returns null
     * when the page of the item was read and the item is not offered anymore, so
     * it shouldn't be purchased. When the page hasn't been read the data already
     * known for the item is kept.
     */
    public static ShopItemData resolve(BackpageAPI backpage, ShopItemData item) {
        if (item == null || item.isEmpty()) {
            return null;
        }

        Map<ShopPage, CachedShopPage> snapshot = pagesOf(backpage);
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

    /**
     * Identifies the shop session the data belongs to. It's null when there's no
     * valid session, in that case no shop data should be used.
     */
    private static String sessionKey(BackpageAPI backpage) {
        if (backpage == null || !backpage.isInstanceValid() || backpage.getInstanceURI() == null) {
            return null;
        }
        return backpage.getUserId() + "|" + backpage.getInstanceURI();
    }

    private static Map<ShopPage, CachedShopPage> pagesOf(BackpageAPI backpage) {
        String sessionKey = sessionKey(backpage);
        if (sessionKey == null) {
            return Collections.emptyMap();
        }

        SessionState session = SESSIONS.get(sessionKey);
        return session == null ? Collections.emptyMap() : session.pages;
    }

    private static SessionState state(String sessionKey) {
        return SESSIONS.computeIfAbsent(sessionKey, key -> new SessionState());
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

    private static void refreshStale(BackpageAPI backpage, SessionState session, String sessionKey) {
        for (ShopPage page : ShopPage.values()) {
            // The account may have changed while the pages were being read
            if (!sessionKey.equals(sessionKey(backpage))) {
                return;
            }

            CachedShopPage cached = session.pages.get(page);
            if (cached != null && cached.isFresh()) {
                continue;
            }

            load(backpage, page, session);
            if (!backpage.isInstanceValid()) {
                return;
            }
        }
    }

    private static void load(BackpageAPI backpage, ShopPage page, SessionState session) {
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

        synchronized (session) {
            Map<ShopPage, CachedShopPage> updated = new EnumMap<>(ShopPage.class);
            updated.putAll(session.pages);
            updated.put(page, items == null
                    ? CachedShopPage.failed(page, session.pages.get(page))
                    : new CachedShopPage(page, items, System.currentTimeMillis(), true));
            session.pages = Collections.unmodifiableMap(updated);
        }
    }

    static void log(String format, Object... args) {
        System.out.println("AutoShop: " + String.format(format, args));
    }
}
