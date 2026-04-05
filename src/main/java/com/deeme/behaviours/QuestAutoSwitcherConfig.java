package com.deeme.behaviours;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

/**
 * Configuration for QuestAutoSwitcher
 */
@Configuration("quest_auto_switcher")
public class QuestAutoSwitcherConfig {
    
    @Option("general.enabled")
    public boolean enabled = true;
    
    @Option("quest_auto_switcher.quest_module_id")
    public String questModuleId = "quest_module";
    
    @Option("quest_auto_switcher.min_priority")
    @Number(min = 1, max = 10, step = 1)
    public int minPriority = 5;
    
    @Option("quest_auto_switcher.max_time_remaining")
    @Number(min = 0, max = 1440, step = 10)
    public int maxTimeRemaining = 60; // minutes, 0 = unlimited
    
    @Option("quest_auto_switcher.max_quest_time")
    @Number(min = 0, max = 300, step = 5)
    public int maxQuestTime = 30; // minutes, 0 = unlimited
    
    @Option("quest_auto_switcher.enable_urgent")
    public boolean enableUrgent = true;
    
    @Option("quest_auto_switcher.enable_event")
    public boolean enableEvent = true;
    
    @Option("quest_auto_switcher.enable_daily")
    public boolean enableDaily = false;
    
    @Option("quest_auto_switcher.enable_weekly")
    public boolean enableWeekly = true;
    
    @Option("quest_auto_switcher.enable_other")
    public boolean enableOther = false;
    
    @Option("quest_auto_switcher.activate_boosters")
    public boolean activateBoosters = false;
    
    @Option("quest_auto_switcher.send_notifications")
    public boolean sendNotifications = true;
    
    @Option("quest_auto_switcher.return_on_death")
    public boolean returnOnDeath = true;
}
