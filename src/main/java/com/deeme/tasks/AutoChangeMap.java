package com.deeme.tasks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.RowSorter;
import javax.swing.SortOrder;

import com.deeme.types.VerifierChecker;
import com.deeme.types.config.MapData;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.core.utils.Lazy;
import com.github.manolo8.darkbot.gui.tree.OptionEditor;
import com.github.manolo8.darkbot.gui.tree.components.InfoTable;
import com.github.manolo8.darkbot.gui.utils.GenericTableModel;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.RepairAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.managers.StarSystemAPI.MapNotFoundException;
import eu.darkbot.api.utils.Inject;

@Feature(name = "AutoChangeMap", description = "Automatically changes map every x amount of time or deaths")
public class AutoChangeMap implements Task, Configurable<AutoChangeMap.ChangeMapConfig> {
    protected final PluginAPI api;
    protected final StatsAPI stats;
    protected final HeroAPI hero;
    protected final RepairAPI repair;
    protected final StarSystemAPI star;

    private ChangeMapConfig changeMapConfig;

    private long waitingTimeNextMap = 0;
    private final Random rand = new Random();
    private int mapMaxDeaths = 0;

    private boolean firstTick = true;

    public AutoChangeMap(PluginAPI api) {
        this(api, api.requireAPI(HeroAPI.class),
                api.requireAPI(StatsAPI.class),
                api.requireAPI(RepairAPI.class),
                api.requireAPI(StarSystemAPI.class),
                api.requireAPI(AuthAPI.class));
    }

    @Inject
    public AutoChangeMap(PluginAPI api, HeroAPI hero, StatsAPI stats, RepairAPI repair, StarSystemAPI star, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);
        
        this.api = api;
        this.hero = hero;
        this.stats = stats;
        this.repair = repair;
        this.star = star;
        setup();
    }

    @Override
    public void setConfig(ConfigSetting<ChangeMapConfig> arg0) {
        this.changeMapConfig = arg0.getValue();
        setup();
    }

    @Override
    public void onTickTask() {
        if (hero.getMap().isGG()) return;

        if (firstTick || (waitingTimeNextMap != 0 && waitingTimeNextMap <= System.currentTimeMillis()) ||
                (mapMaxDeaths > 0 && repair.getDeathAmount() >= mapMaxDeaths) ||
                (waitingTimeNextMap == 0 && mapMaxDeaths == 0)) {
            if (hero.getTarget() == null || hero.getLocalTarget().getHealth().hpPercent() > 90) {
                firstTick = false;
                goNextMap();
            }
        }
        
    }

    private void setup() {
        if (star == null || changeMapConfig == null) return;

        List<String> accessibleMaps = star.getMaps().stream().filter(m -> !m.isGG()).map(m -> m.getName()).toList();

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
        int map;

        for (Map.Entry<String, MapData> oneMap : changeMapConfig.Maps_Changes.entrySet()) {
            if ((oneMap.getValue().time > 0 || oneMap.getValue().deaths > 0) && !oneMap.getKey().equals(hero.getMap().getName())) {
                avaibleMaps.put(oneMap.getKey(), oneMap.getValue());
            }
        }
        int mapChosse = 0;
        if (avaibleMaps.size() > 1) {
            mapChosse = rand.nextInt(avaibleMaps.size());
        }
        int i = 0;
        for (Map.Entry<String, MapData> chosseMap : avaibleMaps.entrySet()) {
            if (i == mapChosse) {
                if (chosseMap.getValue().time > 0) {
                    waitingTimeNextMap = System.currentTimeMillis() +
                            (chosseMap.getValue().time + rand.nextInt(10)) * 60000L;
                }

                if (chosseMap.getValue().deaths > 0) {
                    mapMaxDeaths = repair.getDeathAmount() + chosseMap.getValue().deaths;
                } else {
                    mapMaxDeaths = 0;
                }

                try {
                    map = star.getByName(chosseMap.getKey()).getId();
                    api.requireAPI(ConfigAPI.class).requireConfig("general.working_map").setValue(map);
                } catch (MapNotFoundException e) {
                }

                return;
            }
            i++;
        }
    }

    public static class ChangeMapConfig {
        @Option()
        @Editor(value = JMapChangeTable.class, shared = true)
        public Map<String, MapData> Maps_Changes = new HashMap<>();
        public transient Lazy<String> ADDED_MAPS = new Lazy<>();
    }

    public static class JMapChangeTable extends InfoTable<GenericTableModel, MapData> implements OptionEditor {

        public JMapChangeTable(ChangeMapConfig changeMapConfig) {
            super(MapData.class, changeMapConfig.Maps_Changes, changeMapConfig.ADDED_MAPS, MapData::new);
            getRowSorter().setSortKeys(Arrays.asList(new RowSorter.SortKey(1, SortOrder.DESCENDING),new RowSorter.SortKey(2, SortOrder.DESCENDING)));
        }
    }
}