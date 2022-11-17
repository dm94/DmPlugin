package com.deeme.types.config;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;

@Configuration("special_items")
public class SpecialItems {
    @Option("general.npc_enabled")
    public boolean npcEnabled = false;

    @Option("general.ish")
    public ExtraKeyConditions ish = new ExtraKeyConditions();

    @Option("general.smb")
    public ExtraKeyConditions smb = new ExtraKeyConditions();

    @Option("general.pem")
    public ExtraKeyConditions pem = new ExtraKeyConditions();
}
