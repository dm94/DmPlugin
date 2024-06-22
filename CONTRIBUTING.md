# Contributing

This plugin has a developed open source part to help the Darkbot community and its plugins grow.

## How to contribute

| Contribution      | Details                                                                                      |
| ----------------- | -------------------------------------------------------------------------------------------- |
| Report a bug      | You can [file an issue](https://github.com/dm94/DmPlugin/issues/new/choose) To report a bug. |
| Contribute code   | You can contribute your code by fixing a bug or implementing a feature.                      |
| Help to translate | You can help translate the plugin from [Crowdin](https://crowdin.com/project/dmplugin).      |
| Help in discord   | There are many users asking for help on discord.                                             |

## Contribute Code

To contribute code you can make Pull requests to this repository for review. To do this you have to clone the repository, create a branch, add your code and test it.

### Clone the repository

The repository can be easily cloned using

```bash
$ git clone https://github.com/dm94/DmPlugin.git
```

### Create a branch

[Oficial Github Documentation](https://docs.github.com/articles/creating-and-deleting-branches-within-your-repository)

### Add your code

As a plugin, it has two main dependencies for the code to be executed, those dependencies are [DarkBotAPI](https://github.com/darkbot-reloaded/DarkBotAPI) and [DarkBot](https://github.com/darkbot-reloaded/DarkBot).

That said, I recommend that you have knowledge of how both dependencies work as you need them to make new features and test the plugin's functionality.

One of the main rules when making changes is that the direct use of the DarkBot dependency should be avoided and the correct approach is to use the DarkBotAPI.

### What is the private.jar dependency?

This dependency is a part of the plugin that for now its code is closed source, that said the functions contained in the jar "private.jar" are mocks, that is to say, they are empty functions so that the plugin can be built.

If for some reason you can't make the build because the dependency is out of date please contact me so I can check it and fix it.

### Build the plugin

As I use graddle you can run the task 'copyFile' and it will generate a jar called DmPlugin.jar.

### How to test the changes

Currently the only way to test it is to run the plugin in the bot, the issue is that you probably can't sign it and the bot won't allow you to run it, but you can run the bot from an IDE and test plugins without needing the plugins to be signed. To find out more information about this, it is best to enter the bot's official discord and check the specific channel for this.
