package com.deeme.tasks.autoshop;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.deeme.types.SharedFunctions;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.InstructionProvider;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.managers.BackpageAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.StatsAPI;

public class AutoShop implements Task, Configurable<Config>, InstructionProvider {
    private static final String SHOP_PATH = "ajax/shop.php";
    private static final String INDEX_PATH = "indexInternal.es";

    private final BackpageAPI backpage;
    private final PluginAPI api;
    private final StatsAPI stats;
    private final HeroItemsAPI items;
    private Config config;
    private final JLabel label = new JLabel("Waiting");

    private final Deque<PurchaseTask> purchaseQueue = new ArrayDeque<>();

    public AutoShop(PluginAPI api) throws SecurityException {
        this.api = api;
        this.stats = api.requireAPI(StatsAPI.class);
        this.backpage = api.requireAPI(BackpageAPI.class);
        this.items = api.requireAPI(HeroItemsAPI.class);

        // Warm the shop list in the background, so opening the config is instant
        ShopCatalog.requestRefresh(this.backpage);
    }

    @Override
    public void setConfig(ConfigSetting<Config> arg0) {
        this.config = arg0.getValue();
        migrateLegacyItems(this.config);
    }

    /**
     * Configurations saved by older versions stored only the item id instead of
     * the full shop item, they are converted when the configuration is loaded.
     */
    private void migrateLegacyItems(Config config) {
        if (config == null) {
            return;
        }

        BuyItem[] items = { config.item1, config.item2, config.item3, config.item4, config.item5 };
        for (BuyItem buyItem : items) {
            if (buyItem != null) {
                buyItem.migrateLegacyItem();
            }
        }
    }

    @Override
    public JComponent beforeConfig() {
        return this.label;
    }

    @Override
    public void onTickTask() {
        if (this.config == null || !this.backpage.isInstanceValid()
                || !this.backpage.getSidStatus().contains("OK")) {
            return;
        }

        // Purchases are sent one per tick so a big order does not block the backpage thread
        if (!this.purchaseQueue.isEmpty()) {
            processNextPurchase();
            return;
        }

        // Keeps the item list of the config up to date, loading it in the background
        ShopCatalog.requestRefresh(this.backpage);

        checkItem(this.config.item1);
        checkItem(this.config.item2);
        checkItem(this.config.item3);
        checkItem(this.config.item4);
        checkItem(this.config.item5);
        checkItem(this.config.item6);
        checkItem(this.config.item7);
    }

    private void checkItem(BuyItemConfig itemConfig) {
        if (itemConfig == null || !itemConfig.isEnabled() || itemConfig.getQuantity() <= 0
                || itemConfig.getNextCheck() > System.currentTimeMillis()) {
            return;
        }

        ShopItem shopItem = itemConfig.getShopItem();
        if (shopItem == null || shopItem.getItemId() == null || shopItem.getItemId().isEmpty()) {
            return;
        }

        // Items converted from an old configuration only know their id until the
        // shop list is read, wait for it instead of sending an incomplete purchase
        if (shopItem instanceof ShopItemData && ((ShopItemData) shopItem).getCategory().isEmpty()
                && ShopCatalog.cached(shopItem.getItemId()) == null) {
            return;
        }

        updateCheckTime(itemConfig);

        if (checkNormalCondition(itemConfig) && checkQuantityCondition(itemConfig.getQuantityCondition())) {
            try {
                enqueuePurchase(itemConfig, shopItem);
            } catch (RuntimeException e) {
                updateLabel(shopItem.getDisplayName(), ShopState.PURCHASE_ERROR);
                log("Could not prepare the purchase of %s: %s", shopItem.getDisplayName(), e.getMessage());
            }
        }
    }

    private void enqueuePurchase(BuyItemConfig itemConfig, ShopItem shopItem) {
        ShopItemData data = resolveShopData(shopItem);
        if (data == null) {
            return;
        }

        int amount = itemConfig.getQuantity();
        if (data.dailyLimit >= 0) {
            amount = Math.min(amount, data.dailyLimit);
        }
        if (amount <= 0) {
            return;
        }

        if (!hasEnoughFunds(data, amount)) {
            return;
        }

        if (data.maxAmount > 0 && amount > data.maxAmount) {
            log("Splitting the purchase of %s: total x%d, batch size x%d.",
                    data.getDisplayName(), amount, data.maxAmount);
        }

        // The queue keeps its own copy, the config may be edited while it runs
        ShopItemData purchaseData = new ShopItemData(data);
        int remaining = amount;
        while (remaining > 0) {
            int batch = data.maxAmount > 0 ? Math.min(remaining, data.maxAmount) : remaining;
            this.purchaseQueue.add(new PurchaseTask(purchaseData, batch));
            remaining -= batch;
        }
    }

    private void processNextPurchase() {
        PurchaseTask task = this.purchaseQueue.poll();
        ShopItemData data = task.getData();
        try {
            String response = purchase(data, task.getAmount());
            if (isSuccessful(response)) {
                updateLabel(data.getDisplayName(), ShopState.PURCHASE_SUCCESS);
                log("Purchased %s x%d.", data.getDisplayName(), task.getAmount());
            } else {
                updateLabel(data.getDisplayName(), ShopState.PURCHASE_ERROR);
                log("The server rejected the purchase of %s x%d: %s",
                        data.getDisplayName(), task.getAmount(), shortResponse(response));
            }
        } catch (Exception e) {
            updateLabel(data.getDisplayName(), ShopState.PURCHASE_ERROR);
            log("The purchase of %s x%d failed: %s", data.getDisplayName(), task.getAmount(), e.getMessage());
        }
    }

    private String purchase(ShopItemData data, int amount) throws IOException {
        return this.backpage.postHttp(SHOP_PATH, 3000)
                .setParam("action", "purchase")
                .setParam("category", data.category)
                .setParam("itemId", data.itemId)
                .setParam("amount", amount)
                .setParam("selectedName", "")
                .setParam("level", "")
                .setRawHeader("Referer", referer(data.page))
                .getContent();
    }

    /**
     * The shop answers with a json object, the purchase is only completed when
     * the result field is "success".
     */
    private boolean isSuccessful(String response) {
        if (response == null) {
            return false;
        }

        String trimmed = response.trim();
        if (!trimmed.startsWith("{")) {
            return false;
        }

        try {
            JsonElement result = JsonParser.parseString(trimmed).getAsJsonObject().get("result");
            return result != null && !result.isJsonNull() && "success".equalsIgnoreCase(result.getAsString());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String shortResponse(String response) {
        if (response == null) {
            return "empty response";
        }
        String trimmed = response.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) + "..." : trimmed;
    }

    /**
     * Reads the current shop data of the item, so the category, price and limits
     * always match what the game is selling. The values stored in the config are
     * updated with them.
     */
    private ShopItemData resolveShopData(ShopItem shopItem) {
        boolean fromShop = shopItem instanceof ShopItemData;
        ShopItemData data = fromShop ? (ShopItemData) shopItem : new ShopItemData(shopItem);

        ShopItemData current = ShopCatalog.resolve(data);
        if (current == null) {
            if (fromShop) {
                log("%s is not available in the shop anymore, skipping it.", data.getDisplayName());
                return null;
            }
            // Custom items are not required to be in the shop list, use the config values
            current = data;
        }

        data.copyFrom(current);
        return data;
    }

    private boolean hasEnoughFunds(ShopItemData data, int amount) {
        if (data.currency == null || data.currency.isEmpty() || data.price <= 0) {
            return true;
        }

        double cost = data.price * amount;
        boolean creditPurchase = ShopItemData.CREDITS.equals(data.currency);
        if (!creditPurchase && !ShopItemData.URIDIUM.equals(data.currency)) {
            log("Unsupported currency '%s' for %s, skipping it.", data.currency, data.getDisplayName());
            return false;
        }

        double balance = creditPurchase ? this.stats.getTotalCredits() : this.stats.getTotalUridium();
        if (balance < cost) {
            updateLabel(data.getDisplayName(), creditPurchase ? ShopState.NO_CREDITS : ShopState.NO_URI);
            return false;
        }

        return true;
    }

    private String referer(ShopPage page) {
        StringBuilder builder = new StringBuilder();
        builder.append(this.backpage.getInstanceURI())
                .append(INDEX_PATH)
                .append("?action=")
                .append(page == null ? "internalStart" : "internalDock");
        if (page != null) {
            builder.append("&tpl=").append(page.getTpl());
        }
        return builder.toString();
    }

    private void updateLabel(String itemName, ShopState state) {
        this.label.setText(itemName + " | " + state.getMessage());
    }

    private void updateCheckTime(BuyItemConfig itemConfig) {
        itemConfig.setNextCheck(System.currentTimeMillis() + (itemConfig.getTimeToCheck() * 60_000L));

        LocalDateTime da = LocalDateTime.now();
        ShopItem shopItem = itemConfig.getShopItem();
        String itemName = shopItem != null ? shopItem.getDisplayName() : "Unknown";
        this.label.setText(itemName + " | Last Check: " + da.getHour() + ":" + da.getMinute());
    }

    private boolean checkNormalCondition(BuyItemConfig itemConfig) {
        if (itemConfig.getCondition() == null) {
            return true;
        }

        return itemConfig.getCondition().get(api).allows();
    }

    private boolean checkQuantityCondition(QuantityCondition quantityCondition) {
        if (quantityCondition == null || !quantityCondition.active) {
            return true;
        }

        if (quantityCondition.item == null || quantityCondition.item.isEmpty()) {
            log("The quantity condition has no item selected, skipping the purchase.");
            return false;
        }

        SelectableItem selectableItem = SharedFunctions.getItemById(quantityCondition.item);
        if (selectableItem == null) {
            log("The item '%s' of the quantity condition is not valid, skipping the purchase.",
                    quantityCondition.item);
            return false;
        }

        Item item = this.items.getItem(selectableItem).orElse(null);
        // An item missing from the item bar means the pilot has none left
        double quantity = item == null ? 0 : item.getQuantity();

        return quantity <= quantityCondition.quantity;
    }

    private void log(String format, Object... args) {
        System.out.println("AutoShop: " + String.format(format, args));
    }
}
