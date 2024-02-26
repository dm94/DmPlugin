package com.deeme.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.config.ChangeMapConfig;
import com.deeme.types.config.MapData;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.RepairAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "AutoChangeMap", description = "Automatically changes map every x amount of time or deaths")
public class AutoChangeMap implements Task, Configurable<ChangeMapConfig> {
    private final PluginAPI api;
    private final HeroAPI hero;
    private final RepairAPI repair;
    private final StarSystemAPI star;

    private ChangeMapConfig changeMapConfig;

    private long waitingTimeNextMap = 0;
    private final Random rand = new Random();
    private int mapMaxDeaths = 0;

    private boolean firstTick = true;

    private ArrayList<String> mapsAlreadyUsed = new ArrayList<>();

    public AutoChangeMap(PluginAPI api) {
        this(api, api.requireAPI(HeroAPI.class),
                api.requireAPI(StatsAPI.class),
                api.requireAPI(RepairAPI.class),
                api.requireAPI(StarSystemAPI.class),
                api.requireAPI(AuthAPI.class));
    }

    @Inject
    public AutoChangeMap(PluginAPI api, HeroAPI hero, StatsAPI stats, RepairAPI repair, StarSystemAPI star,
            AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog(api.requireAPI(ExtensionsAPI.class).getFeatureInfo(this.getClass()), auth.getAuthId());

        this.api = api;
        this.hero = hero;
        this.repair = repair;
        this.star = star;
        this.mapsAlreadyUsed = new ArrayList<>();
        setup();
    }

    @Override
    public void setConfig(ConfigSetting<ChangeMapConfig> arg0) {
        this.changeMapConfig = arg0.getValue();
        setup();
    }

    @Override
    public void onTickTask() {
        if (hero.getMap().isGG()) {
            return;
        }

        if ((firstTick || (waitingTimeNextMap != 0 && waitingTimeNextMap <= System.currentTimeMillis()) ||
                (mapMaxDeaths > 0 && repair.getDeathAmount() >= mapMaxDeaths)
                || (waitingTimeNextMap == 0 && mapMaxDeaths == 0)) &&
                (changeMapConfig.ignoreTargetHealth || hero.getLocalTarget() == null || !hero.getLocalTarget().isValid()
                        || hero.getLocalTarget().getHealth().hpPercent() > 90)) {
            firstTick = false;
            goNextMap();
        }

    }

    private void setup() {
        if (star == null || changeMapConfig == null) {
            return;
        }

        List<String> accessibleMaps = star.getMaps().stream().filter(m -> !m.isGG()).map(GameMap::getName)
                .collect(Collectors.toList());

        for (String map : accessibleMaps) {
            MapData info = this.changeMapConfig.Maps_Changes.get(map);
            if (info == null) {
                info = new MapData();
                if (!map.equals("ERROR") && !map.isEmpty()) {
                    changeMapConfig.Maps_Changes.put(map, info);
                    changeMapConfig.ADDED_MAPS.send(map);
                }
            }
        }
    }

    private void goNextMap() {
        HashMap<String, MapData> avaibleMaps = new HashMap<>();

        for (Map.Entry<String, MapData> oneMap : changeMapConfig.Maps_Changes.entrySet()) {
            if ((oneMap.getValue().time > 0 || oneMap.getValue().deaths > 0)
                    && !oneMap.getKey().equals(hero.getMap().getName())) {
                avaibleMaps.put(oneMap.getKey(), oneMap.getValue());
            }
        }
        if (avaibleMaps.size() <= mapsAlreadyUsed.size()) {
            mapsAlreadyUsed.clear();
        } else {
            avaibleMaps.entrySet().removeIf(oneMap -> mapsAlreadyUsed.contains(oneMap.getKey()));
        }

        int mapChosse = avaibleMaps.size() > 1 ? rand.nextInt(avaibleMaps.size()) : 0;
        int i = 0;

        for (Map.Entry<String, MapData> chosseMap : avaibleMaps.entrySet()) {
            if (i == mapChosse) {
                selectMap(chosseMap);
                break;
            }
            i++;
        }
    }

    private void selectMap(Map.Entry<String, MapData> chosseMap) {
        if (chosseMap.getValue().time > 0) {
            waitingTimeNextMap = System.currentTimeMillis() +
                    (chosseMap.getValue().time + (changeMapConfig.addRandomTime ? rand.nextInt(5) : 0))
                            * 60000L;
        } else {
            waitingTimeNextMap = 0;
        }

        if (chosseMap.getValue().deaths > 0) {
            mapMaxDeaths = repair.getDeathAmount() + chosseMap.getValue().deaths;
        } else {
            mapMaxDeaths = 0;
        }

        updateWorkingMap(chosseMap.getKey());
    }

    private void updateWorkingMap(String mapName) {
        GameMap map = star.findMap(mapName).orElse(null);
        if (map == null) {
            return;
        }

        api.requireAPI(ConfigAPI.class).requireConfig("general.working_map").setValue(map.getId());
        mapsAlreadyUsed.add(mapName);
    }
}
