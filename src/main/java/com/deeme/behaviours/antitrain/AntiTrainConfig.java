package com.deeme.behaviours.antitrain;

import com.deeme.types.config.ExtraKeyConditionsSelectable;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("anti_train")
public class AntiTrainConfig {
    @Option("anti_train.max_enemies")
    @Number(min = 0, step = 1)
    public int maxEnemies = 3;

    @Option("anti_train.ignore_distance")
    @Number(min = 0, step = 500, max = 6000)
    public int ignoreDistance = 1500;

    @Option("anti_train.run")
    public boolean run = true;

    @Option("general.item_condition")
    public ExtraKeyConditionsSelectable selectable1 = new ExtraKeyConditionsSelectable();

    @Option("general.item_condition")
    public ExtraKeyConditionsSelectable selectable2 = new ExtraKeyConditionsSelectable();

    @Option("general.item_condition")
    public ExtraKeyConditionsSelectable selectable3 = new ExtraKeyConditionsSelectable();

    @Option("general.item_condition")
    public ExtraKeyConditionsSelectable selectable4 = new ExtraKeyConditionsSelectable();

    @Option("general.item_condition")
    public ExtraKeyConditionsSelectable selectable5 = new ExtraKeyConditionsSelectable();
}
