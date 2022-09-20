package com.deeme.types.gui;

import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Rocket;
import eu.darkbot.api.managers.HeroItemsAPI;

import java.util.ArrayList;
import java.util.List;

import com.github.manolo8.darkbot.config.types.suppliers.OptionList;

public class RocketSupplier extends OptionList<String> {

    protected HeroItemsAPI items;

    @Override
    public String getValue(String id) {
        return id;
    }

    @Override
    public String getText(String id) {
        return id;
    }

    @Override
    public List<String> getOptions() {
        ArrayList<String> allItemsIds = new ArrayList<>();
        Rocket[] selectableItemList = SelectableItem.Rocket.values();
        for (Rocket rocket : selectableItemList) {
            allItemsIds.add(rocket.getId());
        }
        return allItemsIds;
    }
}
