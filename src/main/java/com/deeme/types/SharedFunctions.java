package com.deeme.types;

import com.github.manolo8.darkbot.core.entities.Pet;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcInfo;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.GroupAPI;
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

    public static Ship getAttacker(Ship assaulted, PluginAPI api) {
        HeroAPI hero = api.getAPI(HeroAPI.class);
        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        return getAttacker(assaulted, entities.getShips(), hero);
    }

    public static Ship getAttacker(Ship assaulted, Collection<? extends Ship> allShips, HeroAPI hero) {
        return getAttacker(assaulted, allShips, hero, true);
    }

    public static Ship getAttacker(Ship assaulted, Collection<? extends Ship> allShips, HeroAPI hero,
            boolean onlyEnemies) {
        if (allShips == null || allShips.isEmpty()) {
            return null;
        }

        return allShips.stream()
                .filter(Ship::isValid)
                .filter(s -> s.getId() != hero.getId())
                .filter(s -> s.getEntityInfo().isEnemy() || !onlyEnemies)
                .filter(s -> !(s instanceof Pet))
                .filter(s -> s.isAttacking(assaulted))
                .sorted(Comparator.comparingDouble(s -> s.getLocationInfo().distanceTo(hero)))
                .findFirst().orElse(null);
    }

    public static boolean hasAttacker(Ship assaulted, PluginAPI api) {
        return getAttacker(assaulted, api) != null;
    }

    public static boolean isNpcByName(ConfigAPI config, String name) {
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

    public static GroupMember getMemberGroupAttacked(GroupAPI group, HeroAPI heroapi, ConfigAPI configApi) {
        if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (!member.isDead() && member.getMapId() == heroapi.getMap().getId() && member.isAttacked()
                        && member.getTargetInfo() != null
                        && member.getTargetInfo().getShipType() != 0 && !member.getTargetInfo().getUsername().isEmpty()
                        && !SharedFunctions.isNpcByName(configApi, member.getTargetInfo().getUsername())) {
                    return member;

                }
            }
        }
        return null;
    }

}
