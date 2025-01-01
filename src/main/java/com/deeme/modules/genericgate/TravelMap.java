package com.deeme.modules.genericgate;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import java.util.List;
import java.util.stream.Collectors;

import com.github.manolo8.darkbot.core.manager.StarManager;

@Configuration("travel_map")
public class TravelMap {
    @Option("travel_map.enabled")
    public boolean active = false;

    @Option("travel_map.map")
    @Dropdown(options = MapsDropdown.class)
    public int map = 8;

    public static class MapsDropdown implements Dropdown.Options<Integer> {
        private final StarManager star;
        private final List<Integer> allMaps;

        public MapsDropdown(StarManager star) {
            this.star = star;
            this.allMaps = star.getMaps().stream().map(m -> m.id).collect(Collectors.toList());
        }

        @Override
        public List<Integer> options() {
            return allMaps;
        }

        @Override
        public String getText(Integer option) {
            if (option == null)
                return "";
            return star.byId(option).getName();
        }
    }
}
