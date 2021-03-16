package de.prob2.ui.consoles.b;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.consoles.Console;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.prob2fx.CurrentTrace;

import java.io.File;
import java.util.ResourceBundle;

@FXMLInjected
@Singleton
public final class BConsole extends Console {

	@Inject
	private BConsole(BInterpreter bInterpreter, ResourceBundle bundle, CurrentTrace currentTrace, Config config) {
		super(bundle, bundle.getString("consoles.b.header"), bundle.getString("consoles.b.prompt.classicalB"), bInterpreter);
		currentTrace.stateSpaceProperty().addListener((o, from, to) -> {
			final String message;
			if (to != null) {
				final File modelFile = to.getModel().getModelFile();
				final String name = modelFile == null ? to.getMainComponent().toString() : modelFile.getName();
				message = String.format(bundle.getString("consoles.b.message.modelLoaded"), name);
				String lastLine = this.getText(this.getLineNumber() - 1);
				if(!message.equals(lastLine)) {
					final int oldCaretPos = this.getCaretPosition();
					final String line = message + '\n';
					this.insertText(this.getLineNumber(), 0, line);
					this.moveTo(oldCaretPos + line.length());
					this.requestFollowCaret();
				}
			}
		});
		
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.bConsoleInstructions != null) {
					loadInstructions(configData.bConsoleInstructions);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.bConsoleInstructions = saveInstructions();
			}
		});
	}
}
