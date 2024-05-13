package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.analysis.testcasegeneration.TestCaseGeneratorOperationCoverageSettings;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorSettings;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

public final class OperationCoverageItem extends TestCaseGenerationItem {
	private final List<String> operations;

	@JsonCreator
	public OperationCoverageItem(
		@JsonProperty("id") final String id,
		@JsonProperty("maxDepth") final int maxDepth,
		@JsonProperty("operations") List<String> operations
	) {
		super(id, maxDepth);

		this.operations = new ArrayList<>(operations);
	}

	@Override
	public ValidationTaskType<?> getTaskType() {
		return BuiltinValidationTaskTypes.TEST_CASE_GENERATION_OPERATION_COVERAGE;
	}

	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("animation.testcase.type.coveredOperations");
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
