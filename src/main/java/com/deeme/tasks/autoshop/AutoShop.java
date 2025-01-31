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

        check(this.config.item1);
        check(this.config.item2);
        check(this.config.item3);
        check(this.config.item4);
        check(this.config.item5);
    }

    private void check(BuyItem itemConfig) {
        if (!itemConfig.enable || itemConfig.quantity <= 0 || itemConfig.nextCheck > System.currentTimeMillis()) {
            return;
        }

        updateCheckTime(itemConfig);

        if (itemConfig.itemToBuy != null && checkNormalCondition(itemConfig)
                && checkQuantityCondition(itemConfig.quantityCondition)) {
            tryPurchaseItem(itemConfig);
        }
    }

    private void tryPurchaseItem(BuyItem itemConfig) {
        ItemSupported itemSelected = getItemById(itemConfig.itemToBuy);
        if (itemSelected == null) {
            return;
        }
    
        if (this.stats.getTotalCredits() < itemSelected.getCreditsPrice() * itemConfig.quantity) {
            updateLabel(itemConfig, State.NO_CREDITS);
            return;
        }

        if (this.stats.getTotalUridium() < itemSelected.getUridiumPrice() * itemConfig.quantity) {
            updateLabel(itemConfig, State.NO_URI);
            return;
        }
    
        try {
            buyItem(itemSelected, itemConfig.quantity);
            updateLabel(itemConfig, State.PURCHASE_SUCCESS);
        } catch (Exception e) {
            updateLabel(itemConfig, State.PURCHASE_ERROR);
        }
    }

    private void updateLabel(BuyItem itemConfig, State state) {
        this.updateLabel(itemConfig, state.message);
    }

    private void updateLabel(BuyItem itemConfig, String message) {
        this.label.setText(itemConfig.itemToBuy + " | " + message);
    }

    private void updateCheckTime(BuyItem itemConfig) {
        itemConfig.nextCheck = System.currentTimeMillis() + (itemConfig.timeToCheck * 60000);

        LocalDateTime da = LocalDateTime.now();

        updateLabel(itemConfig, "Last Check: " + da.getHour() + ":" + da.getMinute());
    }

    private boolean checkNormalCondition(BuyItem itemConfig) {
        if (itemConfig.condition == null) {
            return true;
        }

        return itemConfig.condition.get(api).allows();
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

    private void buyItem(ItemSupported item, int quantity) throws IOException, UnsupportedOperationException {
        HttpURLConnection conn = this.backpage.postHttp("ajax/shop.php", 3000)
                .setParam("action", "purchase")
                .setParam("category", item.getCategory())
                .setParam("itemId", item.getId())
                .setParam("amount", quantity)
                .setParam("selectedName", "")
                .setParam("level", "")
                .getConnection();

        if (conn.getResponseCode() != 200) {
            throw new UnsupportedOperationException("Can't connect when sid is invalid");
        }
    }

    private ItemSupported getItemById(String id) {
        ItemSupported[] itemList = ItemSupported.values();
        for (ItemSupported item : itemList) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }
}
