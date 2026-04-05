package com.deeme.behaviours;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.quest.QuestModule;
import com.github.manolo8.darkbot.core.objects.swf.Pair;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.QuestAPI;
import eu.darkbot.api.managers.RepairAPI;
import eu.darkbot.api.objects.quest.Quest;
import eu.darkbot.api.utils.Inject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * QuestAutoSwitcher - Automatically switches to Quest Module when urgent quests appear
 * 
 * Features:
 * - Monitors for urgent/important quests
 * - Automatically switches ship to Quest Module config
 * - Returns to previous config after quest completion
 * - Configurable quest priority levels
 */
@Feature(name = "QuestAutoSwitcher", description = "Auto-switch to Quest Module when urgent quests appear")
public class QuestAutoSwitcher implements Behavior, Configurable<QuestAutoSwitcherConfig> {

    private final PluginAPI api;
    private final BotAPI bot;
    private final HeroAPI hero;
    private final RepairAPI repair;
    private final QuestAPI questAPI;
    private final HeroItemsAPI items;
    private final ExtensionsAPI extensionsAPI;

    private QuestAutoSwitcherConfig config;

    private String previousModule = null;
    private boolean isInQuestMode = false;
    private long questStartTime = 0;
    private Quest currentQuest = null;

    public QuestAutoSwitcher(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class),
                api.requireAPI(BotAPI.class),
                api.requireAPI(HeroAPI.class),
                api.requireAPI(RepairAPI.class),
                api.requireAPI(QuestAPI.class),
                api.requireAPI(HeroItemsAPI.class),
                api.requireAPI(ExtensionsAPI.class));
    }

    @Inject
    public QuestAutoSwitcher(PluginAPI api, AuthAPI auth, BotAPI bot, HeroAPI hero, 
                             RepairAPI repair, QuestAPI questAPI, HeroItemsAPI items,
                             ExtensionsAPI extensionsAPI) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());

        this.api = api;
        this.bot = bot;
        this.hero = hero;
        this.repair = repair;
        this.questAPI = questAPI;
        this.items = items;
        this.extensionsAPI = extensionsAPI;
    }

    @Override
    public void setConfig(QuestAutoSwitcherConfig config) {
        this.config = config;
    }

    @Override
    public void onTickBehavior() {
        if (!config.enabled || repair.isDestroyed()) return;

        // Check if we should switch to quest mode
        if (!isInQuestMode) {
            Quest urgentQuest = findUrgentQuest();
            if (urgentQuest != null) {
                switchToQuestMode(urgentQuest);
            }
        } else {
            // Check if we should return to normal mode
            if (shouldReturnToNormal()) {
                returnToNormalMode();
            } else {
                // Continue quest execution
                updateQuestProgress();
            }
        }
    }

    /**
     * Find an urgent quest that requires immediate attention
     */
    private Quest findUrgentQuest() {
        List<Quest> quests = questAPI.getQuests();
        
        return quests.stream()
                .filter(this::isQuestUrgent)
                .findFirst()
                .orElse(null);
    }

    /**
     * Determine if a quest is urgent based on configuration
     */
    private boolean isQuestUrgent(Quest quest) {
        if (quest == null || quest.isCompleted()) return false;

        // Check if quest type is enabled for auto-switch
        String questType = quest.getType();
        if (!isQuestTypeEnabled(questType)) return false;

        // Check time remaining
        long timeRemaining = quest.getTimeRemaining();
        if (config.maxTimeRemaining > 0 && timeRemaining > config.maxTimeRemaining) {
            return false;
        }

        // Check priority
        int priority = quest.getPriority();
        return priority >= config.minPriority;
    }

    /**
     * Check if quest type is enabled in config
     */
    private boolean isQuestTypeEnabled(String questType) {
        if (questType == null) return false;
        
        switch (questType.toUpperCase()) {
            case "URGENT":
                return config.enableUrgent;
            case "EVENT":
                return config.enableEvent;
            case "DAILY":
                return config.enableDaily;
            case "WEEKLY":
                return config.enableWeekly;
            default:
                return config.enableOther;
        }
    }

    /**
     * Switch to quest mode
     */
    private void switchToQuestMode(Quest quest) {
        // Save current module
        previousModule = bot.getModuleId();
        
        // Record quest info
        currentQuest = quest;
        isInQuestMode = true;
        questStartTime = System.currentTimeMillis();

        // Switch to Quest Module if configured
        if (config.questModuleId != null && !config.questModuleId.isEmpty()) {
            bot.setModule(config.questModuleId);
        }

        // Activate boosters if configured
        if (config.activateBoosters) {
            activateQuestBoosters();
        }

        // Send notification if configured
        if (config.sendNotifications) {
            System.out.println("[QuestAutoSwitcher] Switched to quest mode for: " + quest.getName());
        }
    }

    /**
     * Return to normal mode after quest completion
     */
    private void returnToNormalMode() {
        if (previousModule != null && !previousModule.isEmpty()) {
            bot.setModule(previousModule);
        }

        if (config.sendNotifications) {
            System.out.println("[QuestAutoSwitcher] Quest completed, returned to normal mode");
        }

        // Reset state
        isInQuestMode = false;
        currentQuest = null;
        previousModule = null;
        questStartTime = 0;
    }

    /**
     * Check if we should return to normal mode
     */
    private boolean shouldReturnToNormal() {
        if (currentQuest == null) return true;

        // Quest completed
        if (currentQuest.isCompleted()) return true;

        // Max quest time exceeded
        if (config.maxQuestTime > 0) {
            long elapsed = System.currentTimeMillis() - questStartTime;
            if (elapsed > config.maxQuestTime * 60 * 1000) {
                return true;
            }
        }

        // Ship destroyed
        if (repair.isDestroyed()) return true;

        return false;
    }

    /**
     * Update quest progress and status
     */
    private void updateQuestProgress() {
        if (currentQuest == null) return;

        // Check if current quest still exists and is active
        List<Quest> activeQuests = questAPI.getQuests();
        boolean stillActive = activeQuests.stream()
                .anyMatch(q -> q.getId() == currentQuest.getId() && !q.isCompleted());

        if (!stillActive) {
            // Quest was removed or completed externally
            currentQuest = null;
        }
    }

    /**
     * Activate boosters for quest if configured
     */
    private void activateQuestBoosters() {
        if (items == null) return;

        // Try to activate common boosters
        List<SelectableItem> boosters = items.getItems().stream()
                .filter(item -> item.getId().toLowerCase().contains(" booster"))
                .collect(Collectors.toList());

        for (SelectableItem booster : boosters) {
            if (items.useItem(booster)) {
                break; // Only activate one
            }
        }
    }

    /**
     * Get current status for debugging
     */
    public String getStatus() {
        return String.format(
            "QuestAutoSwitcher[enabled=%s, inQuestMode=%s, quest=%s, previousModule=%s]",
            config.enabled, isInQuestMode, 
            currentQuest != null ? currentQuest.getName() : "none",
            previousModule
        );
    }
}
