package com.deeme.types.gui;

import com.github.manolo8.darkbot.backpage.entities.ShipInfo;
import com.github.manolo8.darkbot.config.types.suppliers.OptionList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShipSupplier extends OptionList<String> {

    private static HashMap<String, String> shipsOwned;

    public ShipSupplier() {
        if (shipsOwned == null) {
            shipsOwned = new HashMap<>();
        }
    }

    public static boolean updateOwnedShips(List<ShipInfo> shipInfos) {
        if (shipInfos == null) return false;
        shipsOwned = new HashMap<>();
        for (ShipInfo e : shipInfos) {
            if (e.getOwned() == 1) {
                shipsOwned.put(e.getLootId().replace("ship_",""),e.getHangarId());
            }
        }

        if (shipsOwned.size() > 0) return true;

        return false;
    }

    @Override
    public String getValue(String name) {
        if (shipsOwned == null || shipsOwned.size() < 1) {
            return name;
        } else {
            return shipsOwned.get(name);
        }
    }

    @Override
    public String getText(String id) {
        for (Map.Entry<String, String> e : shipsOwned.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            if (value.equals(id)) {
                return key;
            }
        }
        return id;
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<>(shipsOwned.keySet());
    }
}
