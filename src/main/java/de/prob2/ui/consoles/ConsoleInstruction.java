package de.prob2.ui.consoles;

public class ConsoleInstruction {
	
	private String instruction;
	private ConsoleInstructionOption option;
	
	public ConsoleInstruction(String instruction, ConsoleInstructionOption option) {
		this.instruction = instruction;
		this.option = option;
	}
	
	public String getInstruction() {
		return instruction;
	}
	
	public ConsoleInstructionOption getOption() {
		return option;
	}
	
	public String toString() {
		return instruction;
	}

}
