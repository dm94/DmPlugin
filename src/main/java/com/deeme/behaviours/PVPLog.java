package com.deeme.behaviours;

import com.deeme.types.VerifierChecker;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.itf.Behaviour;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Feature(name = "PVPLog", description = "Create a log of the PVP battles")
public class PVPLog implements Behaviour {
    public final Path BATTLELOG_FOLDER = Paths.get("battlelog");
    private Main main;
    private HeroManager hero;
    private Ship target = null;


    //Global Data
    private int mapID = 0;
    private long initialTime = 0;
    private boolean win = false;

    //Our Data
    private int shipId = 0;
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

    //Enemy Data
    private String enemyName = "";
    private int enemyShipId = 0;
    private int enemyMaxHp = 0;
    private int enemyInitialHP = 0;
    private int enemyMaxShield = 0;
    private int enemyInitialShield = 0;
    private int enemyInitialHull = 0;
    private int enemyInitialFormation = 0;
    private int enemyInitialSpeed = 0;

    //Batle Data
    private boolean battleStart = false;
    ArrayList<Object> battleData = new ArrayList<Object>();

    Map<String, Object> lastOurData = new HashMap<>();
    Map<String, Object> lastEnemyData = new HashMap<>();

    @Override
    public void install(Main main) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) return;
        VerifierChecker.checkAuthenticity();
        this.main = main;
        this.hero = main.hero;

        try {
            if (!Files.exists(BATTLELOG_FOLDER)) {
                Files.createDirectory(BATTLELOG_FOLDER);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tick() {
        try {
            if (main.isAlive() && main.isRunning() && main.pingManager.ping > 1 && main.hero.getMap() != null) {
                mapID = hero.getMap().getId();
                if (hero.isAttacking() && hero.getLocalTarget() != null && hero.getLocalTarget().getEntityInfo().isEnemy()) {
                    battleStart = true;
                    if (enemyName != hero.getLocalTarget().getEntityInfo().getUsername()) {
                        setInitialEnemyData();
                    }
                    addBattleData();
                } else if (hero.hasTarget()) {
                    setInitialEnemyData();
                } else {
                    if (battleStart) {
                        saveData();
                        battleStart = false;
                    }
                    setOwnInitialData();
                    resetData();
                }
            } else {
                if (battleStart) {
                    saveData();
                    battleStart = false; 
                }
                resetData();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void setOwnInitialData() {
        if (hero.getHealth() != null) {
            shipId = hero.getShipId();
            maxHp = hero.getHealth().getMaxHp();
            initialHP = hero.getHealth().getHp();
            maxShield = hero.getHealth().getMaxShield();
            initialShield = hero.getHealth().getShield();
            initialHull = hero.getHealth().getHull();
            initialLaser = hero.getLaser() != null ? hero.getLaser().getId() : "";
            initialRocket =  hero.getRocket() != null ? hero.getRocket().getId() : "";
            initialConfig = hero.getConfiguration() != null ? hero.getConfiguration().ordinal() : 0;
            initialFormation = hero.getFormation() != null ? hero.getFormation().getId() : "";
            initialSpeed = hero.getSpeed();
            hasPet = hero.hasPet();
        }
    }

    private void setInitialEnemyData() {
        target = main.hero.target;
        if (target != null && target.getHealth() != null) {
            enemyName = hero.getLocalTarget() != null && hero.getLocalTarget().getEntityInfo() != null && hero.getLocalTarget().getEntityInfo().getUsername() != null ? hero.getLocalTarget().getEntityInfo().getUsername() : "";
            enemyShipId = target.getShipId();
            enemyMaxHp = target.getHealth().getMaxHp();
            enemyInitialHP = target.getHealth().getHp();
            enemyMaxShield = target.getHealth().getMaxShield();
            enemyInitialShield = target.getHealth().getShield();
            enemyInitialHull = target.getHealth().getHull();
            enemyInitialFormation = target.formationId;
            enemyInitialSpeed = target.getSpeed();
        }
    }

    private void addBattleData() {
        Map<String, Object> ourData = new HashMap<>();
        ourData.put("hp", hero.getHealth().getHp());
        ourData.put("shield", hero.getHealth().getShield());
        ourData.put("hull", hero.getHealth().getHull());
        ourData.put("laser", hero.getLaser() != null ? hero.getLaser().getId() : "");
        ourData.put("rocket", hero.getRocket() != null ? hero.getRocket().getId() : "");
        ourData.put("config",hero.getConfiguration() != null ? hero.getConfiguration().ordinal() : 0);
        ourData.put("formation", hero.getFormation() != null ? hero.getFormation().getId() : "");
        ourData.put("speed", hero.getSpeed());
        ourData.put("pet", hero.hasPet());

        Map<String, Object> enemyData = new HashMap<>();

        enemyData.put("hp", target.getHealth().getHp());
        enemyData.put("shield", target.getHealth().getShield());
        enemyData.put("hull", target.getHealth().getHull());
        enemyData.put("formation", target.formationId);
        enemyData.put("speed", target.getSpeed());

        if (!lastOurData.equals(ourData) && !lastEnemyData.equals(enemyData)){
            Map<String, Object> globalDetails = new HashMap<>();
            globalDetails.put("time", System.currentTimeMillis());
            globalDetails.put("ourData", ourData);
            globalDetails.put("enemyData", enemyData);
            battleData.add(globalDetails);
        }

        lastOurData = ourData;
        lastEnemyData = enemyData;
    }

    private void resetData() {
        target = null;
        enemyName = "";
        battleStart = false;
        initialTime = System.currentTimeMillis();
        battleData.clear();
    }

    private void saveData() {
        try {
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
            enemyInitialData.put("enemyName", enemyName);
            enemyInitialData.put("enemyShipId", enemyShipId);
            enemyInitialData.put("enemyMaxHp", enemyMaxHp);
            enemyInitialData.put("enemyInitialHP", enemyInitialHP);
            enemyInitialData.put("enemyMaxShield", enemyMaxShield);
            enemyInitialData.put("enemyInitialShield", enemyInitialShield);
            enemyInitialData.put("enemyInitialHull", enemyInitialHull);
            enemyInitialData.put("enemyInitialFormation", enemyInitialFormation);
            enemyInitialData.put("enemyInitialSpeed", enemyInitialSpeed);

            Map<String, Object> globalDetails = new HashMap<>();
            globalDetails.put("date", initialTime);
            globalDetails.put("mapID", mapID);
            globalDetails.put("ownInitialData", ownInitialData);
            globalDetails.put("enemyInitialData", enemyInitialData);
            Collections.reverse(battleData);
            globalDetails.put("battleData", battleData);

            File f = new File("battlelog", mapID+"-"+initialTime+".json");
            Writer writer = new FileWriter(f);
            new Gson().toJson(globalDetails, writer);
            writer.close();
        } catch(IOException e) {
            e.printStackTrace();
        }

    }
}
