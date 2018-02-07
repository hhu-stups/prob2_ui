package de.prob2.ui.consoles.b;

import java.io.File;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.consoles.Console;
import de.prob2.ui.prob2fx.CurrentTrace;

@Singleton
public final class BConsole extends Console {
	@Inject
	private BConsole(BInterpreter bInterpreter, ResourceBundle bundle, CurrentTrace currentTrace) {
		super(bundle, bundle.getString("consoles.b.header"), bundle.getString("consoles.b.prompt.classicalB"), bInterpreter);
		
		currentTrace.addListener((o, from, to) -> {
			final String message;
			if (to == null) {
				message = "Model unloaded";
			} else {
				final File modelFile = to.getStateSpace().getModel().getModelFile();
				final String name = modelFile == null ? to.getStateSpace().getMainComponent().toString() : modelFile.getName();
				message = "Model loaded: " + name;
			}
			this.insertText(this.getLineStart(), message + '\n');
			this.requestFollowCaret();
		});
	}
}
