package com.deeme.modules.sentinel;

/**
 * Abstraction for performing travel/ability actions. Implement this using bot APIs to actually
 * change maps or press abilities/health buttons.
 */
public interface TravelController {
    /**
     * Current map id where the bot is. Used to avoid redundant travel.
     */
    int getCurrentMapId();

    /**
     * Travel to the specified map id.
     */
    void travelToMap(int mapId);

    /**
     * Press heal/emergency button or similar.
     */
    void pressHeal();

    /**
     * Use an ability by id (optional).
     */
    void useAbility(String abilityId);
}
