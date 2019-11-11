package de.prob2.ui.animation.symbolic.testcasegeneration;

import javafx.beans.NamedArg;

public class TestCaseExecutionItem {
	
	private TestCaseGenerationType executionType;

	
	public TestCaseExecutionItem(@NamedArg("executionType") TestCaseGenerationType executionType) {
		this.executionType = executionType;
	}
	
	@Override
	public String toString() {
		return executionType.getName();
	}
	
	public TestCaseGenerationType getExecutionType() {
		return executionType;
	}

}