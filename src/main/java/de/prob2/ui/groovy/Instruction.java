package de.prob2.ui.groovy;

public class Instruction {
	
	private String instruction;
	private InstructionOption option;
	
	public Instruction(String instruction, InstructionOption option) {
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
