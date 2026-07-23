package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.managers.OreAPI;

/**
 * Ore inventory: owned amount per Ore type, sellability flag, trade window
 * open flag, and upgrade slot info (lasers / rockets / speed / shield gens).
 */
public class OreResource implements McpResource {

    private final OreAPI ores;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public OreResource(OreAPI ores) {
        this.ores = ores;
    }

    @Override
    public String getUri() {
        return "mcp://ores";
    }

    @Override
    public String getName() {
        return "Ores & Upgrades";
    }

    @Override
    public String getDescription() {
        return "Owned amount per ore type, sellability, trade window status, and ore upgrade slots.";
    }

    @Override
    public String read(String uri) {
        JsonObject obj = new JsonObject();
        Json.put(obj, "can_sell_ores", ores.canSellOres());

        JsonArray oreArr = new JsonArray();
        for (OreAPI.Ore ore : OreAPI.Ore.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("ore", ore.getName());
            Json.put(o, "amount", ores.getAmount(ore));
            Json.put(o, "sellable", ore.isSellable());
            Json.put(o, "upgradable", ore.isUpgradable());
            oreArr.add(o);
        }
        obj.add("ores", oreArr);

        JsonArray upArr = new JsonArray();
        for (OreAPI.UpgradeSlot slot : OreAPI.UpgradeSlot.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("slot", slot.name());
            OreAPI.Upgrade up = ores.getUpgrade(slot);
            if (up != null) {
                if (up.getOre() != null)
                    o.addProperty("ore", up.getOre().getName());
                Json.put(o, "amount", up.getAmount());
            }
            upArr.add(o);
        }
        obj.add("upgrades", upArr);

        return gson.toJson(obj);
    }
}
