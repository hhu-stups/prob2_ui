package de.prob2.ui.consoles.b;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.consoles.Console;

@Singleton
public final class BConsole extends Console {
		
	@Inject
	private BConsole(BInterpreter interpreter) {
		super("ProB 2.0 B Console");
		this.interpreter = interpreter;
		this.appendText(header + " \n > ");
	}
		
}
