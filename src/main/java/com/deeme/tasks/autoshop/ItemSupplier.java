package com.deeme.tasks.autoshop;

import java.util.ArrayList;
import java.util.List;

import eu.darkbot.api.config.annotations.Dropdown;

public class ItemSupplier implements Dropdown.Options<String> {
    private static ArrayList<String> allItemsIds = new ArrayList<>();

    @Override
    public List<String> options() {
        if (allItemsIds.isEmpty()) {
            ItemSupported[] itemList = ItemSupported.values();
            for (ItemSupported item : itemList) {
                allItemsIds.add(item.getId());
            }
        }

        return allItemsIds;
    }

    @Override
    public String getText(String id) {
        ItemSupported[] itemList = ItemSupported.values();
        for (ItemSupported item : itemList) {
            if (item.getId().equals(id)) {
                return item.getCategory().toUpperCase() + " || " + item.name();
            }
        }
        return id;
    }
}
