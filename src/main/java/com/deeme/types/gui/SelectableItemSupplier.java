package com.deeme.types.gui;

import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.config.annotations.Dropdown;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SelectableItemSupplier implements Dropdown.Options<String> {
    private static ArrayList<String> allItemsIds = new ArrayList<>();

    @Override
    public List<String> options() {
        if (allItemsIds.isEmpty()) {
            Iterator<ItemCategory> it = SelectableItem.ALL_ITEMS.keySet().iterator();
            while (it.hasNext()) {
                Iterator<SelectableItem> itItem = SelectableItem.ALL_ITEMS.get(it.next()).iterator();
                while (itItem.hasNext()) {
                    allItemsIds.add(itItem.next().getId());
                }

            }
        }

        return allItemsIds;
    }

    @Override
    public String getText(String id) {
        Iterator<ItemCategory> it = SelectableItem.ALL_ITEMS.keySet().iterator();
        while (it.hasNext()) {
            ItemCategory category = it.next();
            String itemName = SelectableItem.ALL_ITEMS.get(category).stream()
                    .filter(itItem -> itItem.getId().equals(id) && itItem instanceof Enum)
                    .map(itItem -> (Enum) itItem)
                    .map(itItem -> category + " || " + itItem.name()).findFirst().orElse(null);
            if (itemName != null) {
                return itemName;
            }
        }

        return id;
    }
}
