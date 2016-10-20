package de.prob2.ui.consoles;

import de.prob2.ui.consoles.groovy.InstructionOption;

public class ConsoleInstruction {
	
	private String instruction;
	private InstructionOption option;
	
	public ConsoleInstruction(String instruction, InstructionOption option) {
		this.instruction = instruction;
		this.option = option;
	}
	
	public String getInstruction() {
		return instruction;
	}
	
	public InstructionOption getOption() {
		return option;
	}

}
