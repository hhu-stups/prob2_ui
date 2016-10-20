package de.prob2.ui.consoles;

@FunctionalInterface
public interface Executable {
	public ConsoleExecResult exec(final ConsoleInstruction instruction);
}
