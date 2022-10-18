package com.deeme.types.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.RowSorter;
import javax.swing.SortOrder;

import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.core.utils.Lazy;
import com.github.manolo8.darkbot.gui.tree.OptionEditor;
import com.github.manolo8.darkbot.gui.tree.components.InfoTable;
import com.github.manolo8.darkbot.gui.utils.GenericTableModel;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;

@Configuration("auto_change_map")
public class ChangeMapConfig {

    @Option(value = "auto_change_map.ignote_target_health")
    public boolean ignoreTargetHealth = false;

    @Option(value = "auto_change_map.random_time")
    public boolean addRandomTime = true;

    @Option(value = "auto_change_map.go_to_all_maps")
    public boolean goToAllMaps = false;

    @Option()
    @Editor(value = JMapChangeTable.class, shared = true)
    public Map<String, MapData> Maps_Changes = new HashMap<>();
    public transient Lazy<String> ADDED_MAPS = new Lazy<>();

    public static class JMapChangeTable extends InfoTable<GenericTableModel, MapData> implements OptionEditor {

        public JMapChangeTable(ChangeMapConfig changeMapConfig) {
            super(MapData.class, changeMapConfig.Maps_Changes, changeMapConfig.ADDED_MAPS, MapData::new);
            getRowSorter().setSortKeys(Arrays.asList(new RowSorter.SortKey(1, SortOrder.DESCENDING),
                    new RowSorter.SortKey(2, SortOrder.DESCENDING)));
        }
    }
}
