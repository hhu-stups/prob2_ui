package de.prob2.ui.consoles.b;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.consoles.Console;

@Singleton
public final class BConsole extends Console {
	@Inject
	private BConsole(BInterpreter interpreter) {
		super();
		this.interpreter = interpreter;
		this.appendText("ProB 2.0 B Console \n >");
	}
	
	@Override
	public void reset() {
		this.replaceText("ProB 2.0 B Console");
		this.errors.clear();
	}
	
	@Override
	public void applySettings(String[] settings) {
		super.applySettings(settings);
		if(settings[1].length() != 21) {
			this.appendText("\n ---Engine reseted--- \n >");
		}
	}
}
