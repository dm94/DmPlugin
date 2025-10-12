package com.deeme.tasks.autoshop;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.deeme.types.SharedFunctions;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.InstructionProvider;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.managers.BackpageAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.StatsAPI;

public class AutoShop implements Task, Configurable<Config>, InstructionProvider {
    private final BackpageAPI backpage;
    private final PluginAPI api;
    private final StatsAPI stats;
    private final HeroItemsAPI items;
    private Config config;
    private final JLabel label = new JLabel("Waiting");

    private enum State {
        NO_CREDITS("Not enough credits"),
        NO_URI("Not enough uri"),
        PURCHASE_ERROR("There was an error while purchasing"),
        PURCHASE_SUCCESS("Purchase completed"),
        ;

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    public AutoShop(PluginAPI api) throws SecurityException {
        this.api = api;
        this.stats = api.requireAPI(StatsAPI.class);
        this.backpage = api.requireAPI(BackpageAPI.class);
        this.items = api.requireAPI(HeroItemsAPI.class);
    }

    @Override
    public void setConfig(ConfigSetting<Config> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public JComponent beforeConfig() {
        return this.label;
    }

    @Override
    public void onTickTask() {
        if (!this.backpage.isInstanceValid() || !this.backpage.getSidStatus().contains("OK")) {
            return;
        }

        checkItem(this.config.item1);
        checkItem(this.config.item2);
        checkItem(this.config.item3);
        checkItem(this.config.item4);
        checkItem(this.config.item5);
        checkItem(this.config.item6);
        checkItem(this.config.item7);
    }

    private void checkItem(BuyItemConfig itemConfig) {
        if (!itemConfig.isEnabled() || itemConfig.getQuantity() <= 0 || itemConfig.getNextCheck() > System.currentTimeMillis()) {
            return;
        }

        updateCheckTime(itemConfig);

        ShopItem shopItem = itemConfig.getShopItem();
        if (shopItem != null && checkNormalCondition(itemConfig) && checkQuantityCondition(itemConfig.getQuantityCondition())) {
            tryPurchaseItem(itemConfig, shopItem);
        }
    }

    private void tryPurchaseItem(BuyItemConfig itemConfig, ShopItem shopItem) {
        if (shopItem.getItemId() == null || shopItem.getItemId().isEmpty()) {
            return;
        }

        if (this.stats.getTotalCredits() < shopItem.getCreditsPrice() * itemConfig.getQuantity()) {
            updateLabel(shopItem.getDisplayName(), State.NO_CREDITS);
            return;
        }

        if (this.stats.getTotalUridium() < shopItem.getUridiumPrice() * itemConfig.getQuantity()) {
            updateLabel(shopItem.getDisplayName(), State.NO_URI);
            return;
        }

        try {
            buyItem(shopItem, itemConfig.getQuantity());
            updateLabel(shopItem.getDisplayName(), State.PURCHASE_SUCCESS);
        } catch (Exception e) {
            updateLabel(shopItem.getDisplayName(), State.PURCHASE_ERROR);
        }
    }

    private void updateLabel(String itemName, State state) {
        this.label.setText(itemName + " | " + state.message);
    }

    private void updateCheckTime(BuyItemConfig itemConfig) {
        itemConfig.setNextCheck(System.currentTimeMillis() + (itemConfig.getTimeToCheck() * 60000));

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
        if (!quantityCondition.active) {
            return true;
        }

        if (quantityCondition.item == null) {
            return false;
        }

        Item item = items.getItem(SharedFunctions.getItemById(quantityCondition.item)).orElse(null);

        if (item == null) {
            return false;
        }

        return item.getQuantity() <= quantityCondition.quantity;
    }

    private void buyItem(ShopItem item, int quantity) throws IOException, UnsupportedOperationException {
        HttpURLConnection conn = this.backpage.postHttp("ajax/shop.php", 3000)
                .setParam("action", "purchase")
                .setParam("category", item.getCategory())
                .setParam("itemId", item.getItemId())
                .setParam("amount", quantity)
                .setParam("selectedName", "")
                .setParam("level", "")
                .getConnection();

        if (conn.getResponseCode() != 200) {
            throw new UnsupportedOperationException("Can't connect when sid is invalid");
        }
    }
}
