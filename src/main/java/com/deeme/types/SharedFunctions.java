package com.deeme.types;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.entities.Pet;

import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcInfo;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SharedFunctions {

    private SharedFunctions() {
        throw new IllegalStateException("Utility class");
    }

    public static Ship getAttacker(Ship assaulted, Main main) {
        HeroAPI hero = main.pluginAPI.getAPI(HeroAPI.class);
        EntitiesAPI entities = main.pluginAPI.getAPI(EntitiesAPI.class);
        return getAttacker(assaulted, entities.getShips(), hero);
    }

    public static Ship getAttacker(Ship assaulted, Collection<? extends Ship> allShips, HeroAPI hero) {
        if (allShips == null || allShips.isEmpty()) {
            return null;
        }

        return allShips.stream()
                .filter(s -> (s instanceof Npc || s.getEntityInfo().isEnemy()) && s.getId() != hero.getId())
                .filter(s -> !(s instanceof Pet))
                .filter(s -> s.isAttacking(assaulted))
                .sorted(Comparator.comparingDouble(s -> s.getLocationInfo().distanceTo(hero)))
                .findFirst().orElse(null);
    }

    public static boolean hasAttacker(Ship assaulted, Main main) {
        Ship ship = getAttacker(assaulted, main);
        return ship != null;
    }

    public static boolean isNpc(ConfigAPI config, String name) {
        ConfigSetting<Map<String, NpcInfo>> configSetting = config.requireConfig("loot.npc_infos");
        if (configSetting.getValue() != null) {
            Map<String, NpcInfo> npcInfos = configSetting.getValue();
            NpcInfo info = npcInfos.get(name);
            if (info != null) {
                return true;
            }
        }

        return false;
    }

    public static SelectableItem getItemById(String id) {
        Iterator<ItemCategory> it = SelectableItem.ALL_ITEMS.keySet().iterator();
        while (it.hasNext()) {
            ItemCategory key = it.next();
            List<SelectableItem> selectableItemList = SelectableItem.ALL_ITEMS.get(key);
            Iterator<SelectableItem> itItem = selectableItemList.iterator();
            while (itItem.hasNext()) {
                SelectableItem next = itItem.next();
                if (next.getId().equals(id)) {
                    return next;
                }
            }
        }
        return null;
    }

}
