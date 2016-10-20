package de.prob2.ui.consoles;

public interface Executable<T> {
	public T exec(final ConsoleInstruction instruction);
}
