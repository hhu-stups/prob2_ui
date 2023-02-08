package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.analysis.testcasegeneration.TestCaseGeneratorOperationCoverageSettings;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorSettings;
import de.prob2.ui.verifications.IExecutableItem;

public final class OperationCoverageItem extends TestCaseGenerationItem {
	private final List<String> operations;
	
	@JsonCreator
	public OperationCoverageItem(
		@JsonProperty("maxDepth") final int maxDepth,
		@JsonProperty("operations") List<String> operations
	) {
		super(maxDepth);
		
		this.operations = new ArrayList<>(operations);
	}
	
	@Override
	public TestCaseGenerationType getType() {
		return TestCaseGenerationType.COVERED_OPERATIONS;
	}
	
	public List<String> getOperations() {
		return this.operations;
	}
	
	@Override
	public TestCaseGeneratorSettings getTestCaseGeneratorSettings() {
		return new TestCaseGeneratorOperationCoverageSettings(this.getMaxDepth() - 1, this.getOperations());
	}
	
	@Override
	public String getConfigurationDescription() {
		return "OPERATION:" + String.join(",", this.getOperations()) + "/" + "DEPTH:" + this.getMaxDepth();
	}
	
	@Override
	public boolean settingsEqual(final IExecutableItem obj) {
		if (!(obj instanceof OperationCoverageItem)) {
			return false;
		}
		final OperationCoverageItem other = (OperationCoverageItem)obj;
		return super.settingsEqual(other) && this.getOperations().equals(other.getOperations());
	}
}
