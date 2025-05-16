package com.deeme.modules.guiexecutor;

import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.guiexecutor.GuiExecutorModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;

@Feature(name = "GUI Executor Module [PLUS]",
    description = "Clicks on the configured window. Advanced users")
public class GuiExecutor extends GuiExecutorModule {
  public GuiExecutor(PluginAPI api) throws SecurityException {
    super(api);

    AuthAPI auth = api.requireAPI(AuthAPI.class);
    if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
      throw new SecurityException();
    }

    VerifierChecker.requireAuthenticity(auth);

    ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
    Utils.discordDonorCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());
  }
}
