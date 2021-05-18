package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class OperationCoverageItem extends TestCaseGenerationItem {
	private final List<String> operations;
	
	@JsonCreator
	public OperationCoverageItem(
		@JsonProperty("maxDepth") final int maxDepth,
		@JsonProperty("operations") List<String> operations
	) {
		super(TestCaseGenerationType.COVERED_OPERATIONS, maxDepth);
		
		this.operations = new ArrayList<>(operations);
	}
	
	public List<String> getOperations() {
		return this.operations;
	}
	
	@Override
	public String getConfigurationDescription() {
		return "OPERATION:" + String.join(",", this.getOperations()) + "/" + "DEPTH:" + this.getMaxDepth();
	}
	
	@Override
	public boolean settingsEqual(final TestCaseGenerationItem other) {
		if (!(other instanceof OperationCoverageItem)) {
			return false;
		}
		final OperationCoverageItem o = (OperationCoverageItem)other;
		return super.settingsEqual(o) && this.getOperations().equals(o.getOperations());
	}
}
