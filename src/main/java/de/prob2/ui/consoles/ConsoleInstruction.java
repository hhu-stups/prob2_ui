package de.prob2.ui.consoles;

import java.util.Objects;

public final class ConsoleInstruction {

    private final String instruction;
    private final ConsoleInstructionOption option;

    public ConsoleInstruction(String instruction, ConsoleInstructionOption option) {
        this.instruction = Objects.requireNonNull(instruction);
        this.option = Objects.requireNonNull(option);
    }

    public String getInstruction() {
        return instruction;
    }

    public ConsoleInstructionOption getOption() {
        return option;
    }

    @Override
    public String toString() {
        return instruction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ConsoleInstruction)) {
            return false;
        }

        ConsoleInstruction that = (ConsoleInstruction) o;
        return instruction.equals(that.instruction) && option == that.option;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instruction, option);
    }
}
