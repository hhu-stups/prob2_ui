package de.prob2.ui.consoles.b;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.consoles.Console;

@Singleton
public final class BConsole extends Console {
	@Inject
	private BConsole(BInterpreter interpreter, ResourceBundle bundle) {
		super(bundle.getString("consoles.b.header"), bundle.getString("consoles.b.prompt"));
		this.interpreter = interpreter;
	}
}
