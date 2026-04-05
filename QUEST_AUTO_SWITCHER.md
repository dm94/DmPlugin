# QuestAutoSwitcher

Automatically switches to Quest Module when urgent quests appear, then returns to normal configuration after completion.

## Problem Solved

When using any config/module (e.g., collecting eggs), urgent quests can appear unexpectedly. This module automatically:

1. **Detects urgent quests** (events, dailies, weeklies)
2. **Switches ship** to Quest Module configuration
3. **Completes the quest** 
4. **Returns to previous config** after quest completion

## Features

- ✅ Auto-detect urgent quests by priority
- ✅ Configurable quest types (urgent, event, daily, weekly)
- ✅ Time limit controls (max quest duration)
- ✅ Automatic booster activation (optional)
- ✅ Death detection and safe return
- ✅ Notifications when switching

## Configuration

| Option | Default | Description |
|--------|---------|-------------|
| `enabled` | `true` | Enable the module |
| `quest_module_id` | `"quest_module"` | Module ID for Quest Module |
| `min_priority` | `5` | Minimum quest priority to trigger (1-10) |
| `max_time_remaining` | `60` | Only trigger if quest expires within X minutes |
| `max_quest_time` | `30` | Max time to spend on quest (0 = unlimited) |
| `enable_urgent` | `true` | Auto-switch for urgent quests |
| `enable_event` | `true` | Auto-switch for event quests |
| `enable_daily` | `false` | Auto-switch for daily quests |
| `enable_weekly` | `true` | Auto-switch for weekly quests |
| `enable_other` | `false` | Auto-switch for other quests |
| `activate_boosters` | `false` | Auto-activate boosters for quests |
| `send_notifications` | `true` | Show notifications when switching |
| `return_on_death` | `true` | Return to normal config if ship destroyed |

## Usage Example

```
1. Enable QuestAutoSwitcher in behaviours
2. Set which quest types to auto-accept
3. Configure your Quest Module settings
4. Use any other config normally
5. When urgent quest appears → auto-switches → completes → returns
```

## Bounty

Implements feature request from issue #282

**Use Case:**
- Egg collection event running
- Urgent quest suddenly appears  
- Module auto-switches to quest config
- Completes quest
- Returns to egg collection config

## Files Added
- `QuestAutoSwitcher.java` - Main behaviour
- `QuestAutoSwitcherConfig.java` - Configuration
