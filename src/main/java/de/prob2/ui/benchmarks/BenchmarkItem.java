package de.prob2.ui.benchmarks;

import de.prob2.ui.operations.OperationItem;

public class BenchmarkItem {

	private int position;

	private String name;

	private String time;

	private OperationItem operation;

	public BenchmarkItem(int position, OperationItem operation) {
		this.position = position;
		this.operation = operation;
		this.name = operation.getName();
	}

	public int getPosition() {
		return position;
	}

	public OperationItem getOperation() {
		return operation;
	}

	public String getName() {
		return name;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
}
