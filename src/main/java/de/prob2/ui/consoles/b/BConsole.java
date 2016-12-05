package de.prob2.ui.consoles.b;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.consoles.Console;
import de.prob2.ui.consoles.ConsoleExecResult;
import de.prob2.ui.consoles.ConsoleExecResultType;
import javafx.scene.control.IndexRange;

@Singleton
public class BConsole extends Console {
	
	private BInterpreter interpreter;

	@Inject
	private BConsole(BInterpreter interpreter) {
		super();
		this.interpreter = interpreter;
		this.appendText("ProB 2.0 B Console \n >");
	}

	@Override
	protected void handleEnter() {
		super.handleEnterAbstract();
		String currentLine = getCurrentLine();
		if(currentLine.isEmpty()) {
			this.appendText("\nnull");
		} else {
			ConsoleExecResult execResult = interpreter.exec(instructions.get(posInList));
			this.appendText("\n" + execResult);
			if(execResult.getResultType() == ConsoleExecResultType.ERROR) {
				int begin = this.getText().length() - execResult.toString().length();
				int end = this.getText().length();
				this.setStyleClass(begin, end, "error");
				errors.add(new IndexRange(begin, end));
			}
		}
		this.appendText("\n >");
		this.setStyleClass(this.getText().length() - 2, this.getText().length(), "current");
		this.setEstimatedScrollY(Double.MAX_VALUE);
	}
	
}
