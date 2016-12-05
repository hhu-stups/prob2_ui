package de.prob2.ui.consoles.b;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.consoles.Console;

@Singleton
public class BConsole extends Console {
	
	@Inject
	private BConsole(BInterpreter interpreter) {
		super();
		this.interpreter = interpreter;
		this.appendText("ProB 2.0 B Console \n >");
	}
	
	public void reset() {
		this.replaceText("ProB 2.0 B Console");
		this.errors.clear();
	}
	
}
