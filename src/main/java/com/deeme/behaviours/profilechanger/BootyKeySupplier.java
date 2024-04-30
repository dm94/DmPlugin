package com.deeme.behaviours.profilechanger;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.game.stats.Stats.BootyKey;

import java.util.ArrayList;
import java.util.List;

public class BootyKeySupplier implements Dropdown.Options<String> {

    private static List<String> keyInfos = new ArrayList<String>();

    @Override
    public List<String> options() {
        if (keyInfos.isEmpty()) {
            BootyKey[] bootyValues = BootyKey.values();
            for (BootyKey bootyKey : bootyValues) {
                keyInfos.add(bootyKey.name());
            }
        }

        return keyInfos;
    }

    @Override
    public String getText(String id) {
        return id;
    }
}
