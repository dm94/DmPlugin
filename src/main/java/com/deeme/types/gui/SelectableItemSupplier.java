package com.deeme.types.gui;

import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.managers.HeroItemsAPI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.manolo8.darkbot.config.types.suppliers.OptionList;

public class SelectableItemSupplier extends OptionList<String> {

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
        Iterator<ItemCategory> it = SelectableItem.ALL_ITEMS.keySet().iterator();
        while (it.hasNext()) {
            ItemCategory key = it.next();
            List<SelectableItem> selectableItemList = SelectableItem.ALL_ITEMS.get(key);
            Iterator<SelectableItem> itItem = selectableItemList.iterator();
            while (itItem.hasNext()) {
                allItemsIds.add(itItem.next().getId());
            }

        }
        return allItemsIds;
    }
}
