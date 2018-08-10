package de.prob2.ui.benchmarks;

import java.util.List;
import java.util.Map;

import de.prob2.ui.operations.OperationItem;

public class BenchmarkChoiceItem {

	private OperationItem item;

	public BenchmarkChoiceItem(OperationItem item) {
		this.item = item;
	}

	@Override
	public String toString() {
		return item.getName();
	}

	public String getName() {
		return item.getName();
	}

	public List<String> getParameterNames() {
		return item.getParameterNames();
	}

	public List<String> getParameterValues() {
		return item.getParameterValues();
	}

	public Map<String, String> getVariables() {
		return item.getVariables();
	}

	public Map<String, String> getConstants() {
		return item.getConstants();
	}

	public OperationItem getOperation() {
		return item;
	}
}
