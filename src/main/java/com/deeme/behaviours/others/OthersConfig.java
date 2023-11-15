package com.deeme.behaviours.others;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("others")
public class OthersConfig {
    @Option("others.max_deaths")
    @Number(max = 99, step = 1)
    public int maxDeathsKO = 0;

    @Option("others.reload")
    public boolean reloadIfCrash = false;

    @Option("others.close_bot")
    @Number(max = 120, step = 1)
    public int closeBotMinutes = 0;

    @Option("others.max_memory")
    @Number(max = 6000, step = 100)
    public int maxMemory = 0;

    @Option("others.auto_buy_lcb10")
    public boolean autoBuyLcb10 = false;

    @Option("others.auto_buy_plt_2026")
    public boolean autoBuyPlt2026 = false;

    @Option("others.tag_clan_members")
    public TaggerConfig tagClanMembers = new TaggerConfig();
}
