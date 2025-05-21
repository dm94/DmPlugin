package com.deeme.types.suppliers;

import java.util.ArrayList;
import java.util.List;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.RocketLauncher;

public class RocketLauncherSupplier implements Dropdown.Options<String> {
    private static ArrayList<String> allItemsIds = new ArrayList<>();

    @Override
    public List<String> options() {
        if (allItemsIds.isEmpty()) {
            RocketLauncher[] selectableItemList = SelectableItem.RocketLauncher.values();
            for (RocketLauncher rocket : selectableItemList) {
                allItemsIds.add(rocket.getId());
            }
        }
        return allItemsIds;
    }

    @Override
    public String getText(String id) {
        RocketLauncher[] selectableItemList = SelectableItem.RocketLauncher.values();
        for (RocketLauncher rocket : selectableItemList) {
            if (rocket.getId().equals(id)) {
                return rocket.name();
            }
        }
        return id;
    }
}
