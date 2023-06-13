package com.deeme.behaviours.bestformation;

import java.util.ArrayList;
import java.util.List;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Formation;

public class FormationSupplier implements Dropdown.Options<String> {
    private static ArrayList<String> allItemsIds = new ArrayList<>();

    @Override
    public List<String> options() {
        if (allItemsIds.isEmpty()) {
            Formation[] selectableItemList = SelectableItem.Formation.values();
            for (Formation formation : selectableItemList) {
                allItemsIds.add(formation.getId());
            }
        }
        return allItemsIds;
    }

    @Override
    public String getText(String id) {
        Formation[] selectableItemList = SelectableItem.Formation.values();
        for (Formation formation : selectableItemList) {
            if (formation.getId().equals(id)) {
                return formation.name();
            }
        }
        return id;
    }
}