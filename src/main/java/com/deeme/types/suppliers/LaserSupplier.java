package com.deeme.types.suppliers;

import java.util.ArrayList;
import java.util.List;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Laser;

public class LaserSupplier implements Dropdown.Options<String> {
    private static ArrayList<String> allItemsIds = new ArrayList<>();

    @Override
    public List<String> options() {
        if (allItemsIds.isEmpty()) {
            Laser[] selectableItemList = SelectableItem.Laser.values();
            for (Laser laser : selectableItemList) {
                allItemsIds.add(laser.getId());
            }
        }
        return allItemsIds;
    }

    @Override
    public String getText(String id) {
        Laser[] selectableItemList = SelectableItem.Laser.values();
        for (Laser laser : selectableItemList) {
            if (laser.getId().equals(id)) {
                return laser.name();
            }
        }
        return id;
    }
}
