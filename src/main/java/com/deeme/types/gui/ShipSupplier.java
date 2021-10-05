package com.deeme.types.gui;

import com.github.manolo8.darkbot.backpage.hangar.ShipInfo;
import com.github.manolo8.darkbot.config.types.suppliers.OptionList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ShipSupplier extends OptionList<Integer> {

    private static final Set<ShipSupplier> INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());
    private static Map<Integer, ShipInfo> HANGARS = new HashMap<>();
    private static List<String> SHIPS;

    public static boolean updateOwnedShips(List<ShipInfo> shipInfos) {
        if (shipInfos == null || shipInfos.isEmpty()) return false;

        HANGARS = shipInfos.stream()
                .filter(sh -> sh.getOwned() == 1)
                .collect(Collectors.toMap(ShipInfo::getHangarId, Function.identity()));
        SHIPS = HANGARS.values().stream()
                .map(ShipSupplier::toShipName)
                .collect(Collectors.toList());

        forceUpdate(INSTANCES, SHIPS.size());

        return HANGARS.size() > 0;
    }

    private static String toShipName(ShipInfo ship) {
        return ship.getLootId().replace("ship_", "");
    }

    public ShipSupplier() {
        INSTANCES.add(this);
    }

    @Override
    public Integer getValue(String name) {
        return HANGARS.entrySet()
                .stream()
                .filter(e -> toShipName(e.getValue()).equals(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getText(Integer id) {
        ShipInfo sh = HANGARS.get(id);
        return sh == null ? null : toShipName(sh);
    }

    @Override
    public List<String> getOptions() {
        return SHIPS;
    }
}
