package com.deeme.types.gui;

import com.github.manolo8.darkbot.backpage.hangar.ShipInfo;
import eu.darkbot.api.config.annotations.Dropdown;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ShipSupplier implements Dropdown.Options<Integer> {

    private static Map<Integer, ShipInfo> HANGARS = new HashMap<>();

    public static boolean updateOwnedShips(List<ShipInfo> shipInfos) {
        if (shipInfos == null || shipInfos.isEmpty())
            return false;

        HANGARS = shipInfos.stream()
                .filter(sh -> sh.getOwned() == 1)
                .collect(Collectors.toMap(ShipInfo::getHangarId, Function.identity()));

        return HANGARS.size() > 0;
    }

    private static String toShipName(ShipInfo ship) {
        return ship.getLootId().replace("ship_", "");
    }

    @Override
    public String getText(Integer id) {
        ShipInfo sh = HANGARS.get(id);
        return sh == null ? null : toShipName(sh);
    }

    @Override
    public List<Integer> options() {
        return HANGARS.keySet().stream().collect(Collectors.toList());
    }
}
