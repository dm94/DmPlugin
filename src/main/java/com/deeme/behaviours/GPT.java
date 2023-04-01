package com.deeme.behaviours;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Feature;

@Feature(name = "AI GPT", description = "It will automatically read the environment and make changes that will improve the bot's tasks.")
public class GPT implements Behavior {
    public GPT(PluginAPI api) {
        System.out.println("Happy April Fools' Day! This is just a prank. :)");
        System.out.println("                ___");
        System.out.println("            . -^   `--,");
        System.out.println("           /#         /#\\");
        System.out.println("          /#`__      /#`_\\");
        System.out.println("         |#   `\\    |#   `\\");
        System.out.println("         |#    |    |#    |");
        System.out.println("         |#    |    |#    |");
        System.out.println("         |#    |    |#    |");
        System.out.println("         |#    |    |#    |");
        System.out.println("         |#    |    |#    |");
        System.out.println("         |#    |____|#    |");
        System.out.println("         |#  `/      |#  `/ ");
        System.out.println("        /#  /        /#  /  ");
        System.out.println("        ~~~~         ~~~~   ");
    }

    @Override
    public void onTickBehavior() {
    }
}
