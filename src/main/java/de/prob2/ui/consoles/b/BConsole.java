package de.prob2.ui.consoles.b;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.consoles.Console;
import javafx.scene.input.KeyEvent;

@Singleton
public class BConsole extends Console {
	
	private BInterpreter interpreter;

	@Inject
	private BConsole(BInterpreter interpreter) {
		super();
		this.interpreter = interpreter;
		this.appendText("Prob 2.0 B Console \n >");
	}

	@Override
	protected void handleEnter(KeyEvent e) {
		super.handleEnterAbstract(e);
		if(!getCurrentLine().isEmpty()) {
			this.appendText("\n" + interpreter.exec(instructions.get(posInList)));
		}
		this.appendText("\n >");
	}
	
}
