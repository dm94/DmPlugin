package com.deeme.behaviours.bestrocket;

import java.util.ArrayList;
import java.util.List;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Rocket;

public class RocketSupplier implements Dropdown.Options<String> {
    private static ArrayList<String> allItemsIds = new ArrayList<>();

    @Override
    public List<String> options() {
        if (allItemsIds.isEmpty()) {
            Rocket[] selectableItemList = SelectableItem.Rocket.values();
            for (Rocket rocket : selectableItemList) {
                allItemsIds.add(rocket.getId());
            }
        }
        return allItemsIds;
    }

    @Override
    public String getText(String id) {
        Rocket[] selectableItemList = SelectableItem.Rocket.values();
        for (Rocket rocket : selectableItemList) {
            if (rocket.getId().equals(id)) {
                return rocket.name();
            }
        }
        return id;
    }
}
