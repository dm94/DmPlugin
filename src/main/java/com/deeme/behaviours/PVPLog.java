package com.deeme.behaviours;

import com.deeme.types.VerifierChecker;
import com.google.gson.Gson;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.RepairAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Feature(name = "PVPLog", description = "Create a log of the PVP battles")
public class PVPLog implements Behavior, Task {
    public final Path battleLogFolder = Paths.get("battlelog");
    private Player target = null;

    protected final PluginAPI api;
    protected final StatsAPI stats;
    protected final HeroAPI hero;
    protected final RepairAPI repair;
    protected final HeroItemsAPI items;
    protected Collection<? extends Player> players;

    private Gson gson;

    // Global Data
    private int mapID = 0;
    private long initialTime = 0;
    private boolean win = false;
    private int deaths = 0;

    // Our Data
    private String shipId = "";
    private int maxHp = 0;
    private int initialHP = 0;
    private int maxShield = 0;
    private int initialShield = 0;
    private int initialHull = 0;
    private String initialLaser = "";
    private String initialRocket = "";
    private int initialConfig = 0;
    private String initialFormation = "";
    private int initialSpeed = 0;
    private boolean hasPet = false;

    // Enemy Data
    private int enemyId = 0;
    private String enemyShipType = "";
    private int enemyMaxHp = 0;
    private int enemyInitialHP = 0;
    private int enemyMaxShield = 0;
    private int enemyInitialShield = 0;
    private int enemyInitialHull = 0;

    // Batle Data
    private boolean battleStart = false;
    ArrayList<Object> battleData = new ArrayList<Object>();

    Map<String, Object> lastOurData = new HashMap<>();
    Map<String, Object> lastEnemyData = new HashMap<>();

    private ArrayList<String> saveQueue = new ArrayList<String>();

    public PVPLog(PluginAPI api) {
        this(api, api.requireAPI(HeroAPI.class),
                api.requireAPI(StatsAPI.class),
                api.requireAPI(RepairAPI.class),
                api.requireAPI(AuthAPI.class),
                api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public PVPLog(PluginAPI api, HeroAPI hero, StatsAPI stats, RepairAPI repair, AuthAPI auth, HeroItemsAPI items) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        this.api = api;
        this.hero = hero;
        this.stats = stats;
        this.repair = repair;
        this.items = items;
        this.gson = new Gson();

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.players = entities.getPlayers();

        try {
            if (!Files.exists(battleLogFolder)) {
                Files.createDirectory(battleLogFolder);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTickTask() {
        saveData();
    }

    @Override
    public void onTickBehavior() {
        if (hero.getMap() != null && hero.getMap().isGG()) {
            return;
        }
        try {
            if (stats.getPing() > 1 && hero.getMap() != null) {
                mapID = hero.getMap().getId();

                if (hero.isAttacking() && hero.getLocalTarget() != null
                        && hero.getLocalTarget().getEntityInfo().isEnemy()
                        && !hero.getLocalTarget().getEntityInfo().getUsername().contains("Saturn")) {
                    battleStart = true;
                    if (enemyId != hero.getLocalTarget().getId()) {
                        setInitialEnemyData();
                    }
                    addBattleData();
                } else if (hero.getTarget() != null) {
                    setInitialEnemyData();
                } else {
                    resetData();
                    setOwnInitialData();
                }
            } else {
                resetData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Player getEnemy() {
        Lockable enemy = hero.getLocalTarget();
        if (enemy != null && enemy.getHealth() != null) {
            return players.stream()
                    .filter(s -> (enemy.getId() == s.getId()))
                    .findAny().orElse(null);
        }

        return null;
    }

    private void setOwnInitialData() {
        if (hero.getHealth() != null) {
            shipId = hero.getShipType();
            maxHp = hero.getHealth().getMaxHp();
            initialHP = hero.getHealth().getHp();
            maxShield = hero.getHealth().getMaxShield();
            initialShield = hero.getHealth().getShield();
            initialHull = hero.getHealth().getHull();
            initialLaser = hero.getLaser() != null ? hero.getLaser().getId() : "";
            initialRocket = hero.getRocket() != null ? hero.getRocket().getId() : "";
            initialConfig = hero.getConfiguration() != null ? hero.getConfiguration().ordinal() : 0;
            initialFormation = hero.getFormation() != null ? hero.getFormation().getId() : "";
            initialSpeed = hero.getSpeed();
            hasPet = hero.hasPet();
        }
    }

    private void setInitialEnemyData() {
        target = getEnemy();
        if (target != null) {
            enemyId = target.getId();
            enemyShipType = target.getShipType();
            enemyMaxHp = target.getHealth().getMaxHp();
            enemyInitialHP = target.getHealth().getHp();
            enemyMaxShield = target.getHealth().getMaxShield();
            enemyInitialShield = target.getHealth().getShield();
            enemyInitialHull = target.getHealth().getHull();
        }
    }

    private void addBattleData() {
        Map<String, Object> ourData = new HashMap<>();
        ourData.put("hp", hero.getHealth().getHp());
        ourData.put("shield", hero.getHealth().getShield());
        ourData.put("hull", hero.getHealth().getHull());
        ourData.put("laser", hero.getLaser() != null ? hero.getLaser().getId() : "");
        ourData.put("rocket", hero.getRocket() != null ? hero.getRocket().getId() : "");
        ourData.put("config", hero.getConfiguration() != null ? hero.getConfiguration().ordinal() : 0);
        ourData.put("formation", hero.getFormation() != null ? hero.getFormation().getId() : "");
        ourData.put("speed", hero.getSpeed());
        ourData.put("pet", hero.hasPet());
        ourData.put("effects", hero.getEffects().toString());
        ourData.put("items", getOurSpecialItems());

        Map<String, Object> enemyData = new HashMap<>();
        enemyData.put("hp", target.getHealth().getHp());
        enemyData.put("shield", target.getHealth().getShield());
        enemyData.put("hull", target.getHealth().getHull());
        enemyData.put("effects", target.getEffects().toString());
        enemyData.put("formation", target.getFormation() != null ? target.getFormation().getId() : "");
        enemyData.put("speed", target.getSpeed());
        enemyData.put("pet", target.hasPet());

        if (!lastOurData.equals(ourData) && !lastEnemyData.equals(enemyData)) {
            Map<String, Object> globalDetails = new HashMap<>();
            globalDetails.put("time", System.currentTimeMillis());
            globalDetails.put("ourData", ourData);
            globalDetails.put("enemyData", enemyData);
            battleData.add(globalDetails);
        }

        lastOurData = ourData;
        lastEnemyData = enemyData;
    }

    private Map<String, Object> getOurSpecialItems() {
        Map<String, Object> ourItems = new HashMap<>();

        List<Item> usableItems = items.getItems(ItemCategory.SPECIAL_ITEMS).stream().filter(Item::isUsable)
                .filter(Item::isAvailable)
                .collect(Collectors.toList());
        for (Item item : usableItems) {
            try {
                if (item.lastUseTime() != 0) {
                    ourItems.put(item.getId(), item.getTimer());
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }

        return ourItems;
    }

    private void resetData() {
        target = null;
        if (battleStart || !battleData.isEmpty()) {
            if (deaths == repair.getDeathAmount()) {
                win = true;
            }
            insertToQueue();
            battleStart = false;
        }
        battleData.clear();
        enemyId = 0;
        initialTime = System.currentTimeMillis();
        deaths = repair.getDeathAmount();
        win = false;
    }

    private void insertToQueue() {
        Map<String, Object> ownInitialData = new HashMap<>();
        ownInitialData.put("shipId", shipId);
        ownInitialData.put("maxHp", maxHp);
        ownInitialData.put("initialHP", initialHP);
        ownInitialData.put("maxShield", maxShield);
        ownInitialData.put("initialShield", initialShield);
        ownInitialData.put("initialHull", initialHull);
        ownInitialData.put("initialLaser", initialLaser);
        ownInitialData.put("initialRocket", initialRocket);
        ownInitialData.put("initialConfig", initialConfig);
        ownInitialData.put("initialFormation", initialFormation);
        ownInitialData.put("initialSpeed", initialSpeed);
        ownInitialData.put("hasPet", hasPet);

        Map<String, Object> enemyInitialData = new HashMap<>();
        enemyInitialData.put("enemyShipTipe", enemyShipType);
        enemyInitialData.put("enemyMaxHp", enemyMaxHp);
        enemyInitialData.put("enemyInitialHP", enemyInitialHP);
        enemyInitialData.put("enemyMaxShield", enemyMaxShield);
        enemyInitialData.put("enemyInitialShield", enemyInitialShield);
        enemyInitialData.put("enemyInitialHull", enemyInitialHull);

        Map<String, Object> globalDetails = new HashMap<>();
        globalDetails.put("date", initialTime);
        globalDetails.put("endDate", System.currentTimeMillis());
        globalDetails.put("mapID", mapID);
        globalDetails.put("battleWon", win);
        globalDetails.put("battleData", battleData);
        globalDetails.put("ownInitialData", ownInitialData);
        globalDetails.put("enemyInitialData", enemyInitialData);

        saveQueue.add(gson.toJson(globalDetails));
    }

    private void saveData() {
        try {
            if (saveQueue.size() > 0) {
                String data = saveQueue.get(0);
                saveQueue.remove(0);

                File f = new File("battlelog", System.currentTimeMillis() + ".json");
                Writer writer = new FileWriter(f);
                writer.write(data);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
