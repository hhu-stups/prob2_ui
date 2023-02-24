package de.prob2.ui.consoles.b;

import java.io.File;
import java.util.Collections;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.consoles.Console;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentTrace;

@FXMLInjected
@Singleton
public final class BConsole extends Console {

	@Inject
	private BConsole(BInterpreter bInterpreter, I18n i18n, CurrentTrace currentTrace, Config config) {
		super(i18n, bInterpreter, "consoles.b.header", "consoles.b.prompt.classicalB");
		currentTrace.stateSpaceProperty().addListener((o, from, to) -> {
			if (to != null) {
				final File modelFile = to.getModel().getModelFile();
				final String name = modelFile == null ? to.getMainComponent().toString() : modelFile.getName();
				final String message = i18n.translate("consoles.b.message.modelLoaded", name);
				this.addParagraph(message, Collections.singletonList("message"));
			}
		});

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.bConsoleInstructions != null) {
					setHistory(configData.bConsoleInstructions);
				}
			}

			@Override
			public void saveConfig(final ConfigData configData) {
				configData.bConsoleInstructions = getHistory();
			}
		});
	}
}
