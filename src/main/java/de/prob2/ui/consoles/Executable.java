package de.prob2.ui.consoles;

@FunctionalInterface
public interface Executable<T> {
	public T exec(final ConsoleInstruction instruction);
}
