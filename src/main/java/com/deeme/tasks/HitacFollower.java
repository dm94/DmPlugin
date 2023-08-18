package com.deeme.tasks;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.config.HitacFollowerConfig;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.events.EventHandler;
import eu.darkbot.api.events.Listener;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.GameLogAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "HitacFollower", description = "Change the main map to where the Hitac is located")
public class HitacFollower implements Task, Listener, Configurable<HitacFollowerConfig> {

    protected final PluginAPI api;
    protected final BotAPI bot;
    protected final HeroAPI hero;
    protected final GameLogAPI log;
    protected final StarSystemAPI star;
    protected final ExtensionsAPI extensionsAPI;
    protected final Collection<? extends Npc> npcs;

    protected final Pattern pattern = Pattern.compile("[0-9]+-[0-9]+", Pattern.CASE_INSENSITIVE);

    private HitacFollowerConfig followerConfig;

    private final Deque<String> hitacAliensMaps = new LinkedList<>();

    private long nextCheck = 0;
    private boolean mapHasHitac = false;

    public HitacFollower(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class),
                api.requireAPI(BotAPI.class),
                api.requireAPI(HeroAPI.class),
                api.requireAPI(StarSystemAPI.class),
                api.requireAPI(GameLogAPI.class),
                api.requireAPI(EntitiesAPI.class));
    }

    @Inject
    public HitacFollower(PluginAPI api, AuthAPI auth, BotAPI bot, HeroAPI hero, StarSystemAPI star,
                         GameLogAPI log, EntitiesAPI entities) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        this.extensionsAPI = api.getAPI(ExtensionsAPI.class);
        Utils.showDonateDialog();

        this.api = api;
        this.bot = bot;
        this.hero = hero;
        this.star = star;
        this.log = log;

        this.npcs = entities.getNpcs();
    }

    @Override
    public void setConfig(ConfigSetting<HitacFollowerConfig> arg0) {
        this.followerConfig = arg0.getValue();
    }

    @Override
    public void onTickTask() {
        if (followerConfig.enable && nextCheck <= System.currentTimeMillis()) {
            nextCheck = System.currentTimeMillis() + 20000;
            if (hasHitac()) {
                mapHasHitac = true;
            } else {
                if (mapHasHitac) {
                    mapHasHitac = false;
                    goToNextMap();
                } else if (!hitacAliensMaps.isEmpty()) {
                    changeMap(hitacAliensMaps.getFirst());
                } else if (followerConfig.returnToWaitingMap) {
                    api.requireAPI(ConfigAPI.class).requireConfig("general.working_map")
                            .setValue(followerConfig.waitMap);
                }
            }

            //remove if current map is the map next map visit
            if (hitacAliensMaps.getFirst().equalsIgnoreCase(star.getCurrentMap().getShortName())) {
                hitacAliensMaps.removeFirst();
            }
        }
    }

    @EventHandler
    public void onLogMessage(GameLogAPI.LogMessageEvent message) {
        if (followerConfig.enable && extensionsAPI.getFeatureInfo(this.getClass()).isEnabled()) {
            String msg = message.getMessage();

            if (msg.contains("Hitac") && titleFilter(msg) && pvpFilter(msg)) {
                Matcher matcher = pattern.matcher(msg);
                if (matcher.find()) {
                    addSpawnHitac(matcher.group(0));
                }
            }
        }
    }

    private boolean pvpFilter(String message) {
        return followerConfig.goToPVP || !message.contains("PvP");
    }

    private boolean titleFilter(String message) {
        return followerConfig.goForTheTitle || !(message.contains("Hitac-Underling")
                && message.contains("Hitac-Underboss"));
    }

    private void addSpawnHitac(String map) {
        //add map if not in list
        if (hitacAliensMaps.stream().noneMatch(alien -> alien.equalsIgnoreCase(this.star.getCurrentMap().getName()))) {
            hitacAliensMaps.add(map);
        }
        //add next map it will jump to

        String nextMap;
        if ((nextMap = getNextMap(map)) != null) {
            hitacAliensMaps.add(nextMap);
        }
    }

    private void goToNextMap() {
        String nextMap;
        if ((nextMap = getNextMap(hero.getMap().getName())) != null) {
            changeMap(nextMap);
        }
    }

    private String getNextMap(String givenMap) {
        String nextMap;
        switch (givenMap) {
            case "1-3":
                nextMap = "1-4";
                break;
            case "1-4":
                nextMap = "3-4";
                break;
            case "3-4":
                nextMap = "3-3";
                break;
            case "3-3":
                nextMap = "2-4";
                break;
            case "2-4":
                nextMap = "2-3";
                break;
            case "2-3":
                nextMap = "1-3";
                break;
            case "4-1":
                nextMap = "4-3";
                break;
            case "4-2":
                nextMap = "4-1";
                break;
            case "4-3":
                nextMap = "4-2";
                break;
            default:
                nextMap = null;
        }
        return nextMap;
    }

    private boolean hasHitac() {
        if (npcs == null || npcs.isEmpty()) {
            return false;
        }
        return npcs.stream()
                .anyMatch(s -> (s.getEntityInfo() != null
                        && s.getEntityInfo().getUsername() != null
                        && s.getEntityInfo().getUsername().contains("Hitac")
                        && !s.getEntityInfo().getUsername().contains("Hitac-Underling")
                        && !s.getEntityInfo().getUsername().contains("Hitac-Underboss")));
    }

    private void changeMap(String mapName) {
        if (mapName.contains("-3") || mapName.contains("-4")) {
            if (!followerConfig.lowers || (!followerConfig.lowerEnemy && !mapName.contains(this.hero.getEntityInfo().getFaction().ordinal() + "-"))) {
                if (hitacAliensMaps.getFirst().equalsIgnoreCase(mapName)) {
                    hitacAliensMaps.removeFirst();
                }
                return;
            }
        }

        if (mapName.contains("-5") || mapName.contains("-6") || mapName.contains("-7") || mapName.contains("-8")) {
            if (!followerConfig.uppers || (!followerConfig.upperEnemy && !mapName.contains(this.hero.getEntityInfo().getFaction().ordinal() + "-"))) {
                if (hitacAliensMaps.getFirst().equalsIgnoreCase(mapName)) {
                    hitacAliensMaps.removeFirst();
                }
                return;
            }
        }

        GameMap nextMap = star.findMap(mapName).orElse(null);
        if (nextMap == null) {
            return;
        }
        api.requireAPI(ConfigAPI.class).requireConfig("general.working_map").setValue(nextMap.getId());
    }
}