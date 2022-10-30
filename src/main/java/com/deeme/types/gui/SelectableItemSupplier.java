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
                ItemCategory key = it.next();
                List<SelectableItem> selectableItemList = SelectableItem.ALL_ITEMS.get(key);
                Iterator<SelectableItem> itItem = selectableItemList.iterator();
                while (itItem.hasNext()) {
                    allItemsIds.add(itItem.next().getId());
                }

            }
        }

        return allItemsIds;
    }

    @Override
    public String getText(String id) {
        return id;
    }
}
