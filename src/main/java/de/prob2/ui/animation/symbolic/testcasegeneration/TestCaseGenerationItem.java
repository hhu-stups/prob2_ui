package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorResult;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorSettings;
import de.prob.analysis.testcasegeneration.testtrace.TestTrace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.ICheckingResult;
import de.prob2.ui.verifications.ICliTask;

@JsonPropertyOrder({
	"id",
	"maxDepth",
	"selected",
})
public abstract class TestCaseGenerationItem extends AbstractCheckableItem implements ICliTask {
	public static final class Result implements ICheckingResult {
		private final TestCaseGeneratorResult result;
		private final List<Trace> traces;

		public Result(TestCaseGeneratorResult result) {
			this.result = Objects.requireNonNull(result, "result");

			this.traces = new ArrayList<>();
			for (TestTrace testTrace : result.getTestTraces()) {
				if (testTrace.getTrace() != null) {
					this.traces.add(testTrace.getTrace());
				}
			}
		}

		@Override
		public CheckingStatus getStatus() {
			if (this.getResult().isInterrupted()) {
				return CheckingStatus.INTERRUPTED;
			} else if (this.getResult().getTestTraces().isEmpty() || !this.getResult().getUncoveredTargets().isEmpty()) {
				return CheckingStatus.FAIL;
			} else {
				return CheckingStatus.SUCCESS;
			}
		}

		@Override
		public String getMessageBundleKey() {
			if (this.getResult().isInterrupted()) {
				return "animation.testcase.result.interrupted";
			} else if (this.getResult().getTestTraces().isEmpty()) {
				return "animation.testcase.result.notFound";
			} else if (!this.getResult().getUncoveredTargets().isEmpty()) {
				return "animation.testcase.result.notAllGenerated";
			} else {
				return "animation.testcase.result.found";
			}
		}

		@Override
		public List<Trace> getTraces() {
			return Collections.unmodifiableList(this.traces);
		}

		public TestCaseGeneratorResult getResult() {
			return this.result;
		}

		@Override
		public ICheckingResult withoutAnimatorDependentState() {
			return new CheckingResult(this.getStatus(), this.getMessageBundleKey(), this.getMessageParams());
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;

	private final int maxDepth;

	protected TestCaseGenerationItem(final String id, final int maxDepth) {
		super();
		this.id = id;
		this.maxDepth = maxDepth;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getTaskDescription(I18n i18n) {
		return this.getConfigurationDescription();
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	@JsonIgnore
	public abstract TestCaseGeneratorSettings getTestCaseGeneratorSettings();

	@JsonIgnore
	public abstract String getConfigurationDescription();

	@Override
	public void execute(final ExecutionContext context) {
		ConstraintBasedTestCaseGenerator cbTestCaseGenerator = new ConstraintBasedTestCaseGenerator(context.stateSpace(), this.getTestCaseGeneratorSettings(), new ArrayList<>());
		TestCaseGeneratorResult res = cbTestCaseGenerator.generateTestCases();
		this.setResult(new TestCaseGenerationItem.Result(res));
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof TestCaseGenerationItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getMaxDepth(), that.getMaxDepth());
	}
}
