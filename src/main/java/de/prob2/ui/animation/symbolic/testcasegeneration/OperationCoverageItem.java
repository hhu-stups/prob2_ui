package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.analysis.testcasegeneration.TestCaseGeneratorOperationCoverageSettings;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorSettings;

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
	public boolean settingsEqual(Object other) {
		return other instanceof OperationCoverageItem that
			       && super.settingsEqual(that)
			       && Objects.equals(this.getOperations(), that.getOperations());
	}
}
