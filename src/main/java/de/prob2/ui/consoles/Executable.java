package de.prob2.ui.consoles;

@FunctionalInterface
public interface Executable {
    ConsoleExecResult exec(final ConsoleInstruction instruction);
}
