package de.prob2.ui.consoles;

public record ConsoleExecResult(String consoleOutput, String result, ConsoleExecResultType resultType) {

	@Override
	public String toString() {
		return consoleOutput + result;
	}
}
