package com.deeme.behaviours.others;

import eu.darkbot.api.config.annotations.Option;

import com.github.manolo8.darkbot.config.PlayerTag;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Tag;

@Configuration("tagger")
public class TaggerConfig {
    @Option("general.enabled")
    public boolean active = false;

    @Option("tagger.tag")
    public @Tag(Tag.Default.NONE) PlayerTag clanTag = null;
}
