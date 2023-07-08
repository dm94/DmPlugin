package com.deeme.tasks.discord;

import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

import eu.darkbot.api.config.annotations.Configuration;

@Configuration("discord")
public class DiscordConfig {
    @Option("discord.message_interval")
    @Number(min = 10, max = 500)
    public int intervalMessage = 10;

    @Option("discord.discord_webhook")
    public String discordWebHook = null;

    @Option("discord.send_sid")
    public boolean sendSidLink = false;
}
