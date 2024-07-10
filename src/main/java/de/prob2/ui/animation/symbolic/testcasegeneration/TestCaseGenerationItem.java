package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
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
import de.prob2.ui.verifications.ICliTask;
import de.prob2.ui.verifications.ITraceTask;
import de.prob2.ui.verifications.TraceResult;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@JsonPropertyOrder({
	"id",
	"maxDepth",
	"selected",
})
public abstract class TestCaseGenerationItem extends AbstractCheckableItem implements ICliTask, ITraceTask {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;

	private final int maxDepth;

	@JsonIgnore
	private final ObjectProperty<TestCaseGeneratorResult> generatorResult = new SimpleObjectProperty<>(this, "generatorResult", null);

	@JsonIgnore
	private final ListProperty<Trace> examples = new SimpleListProperty<>(this, "examples", FXCollections.observableArrayList());

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

	public ObjectProperty<TestCaseGeneratorResult> generatorResultProperty() {
		return this.generatorResult;
	}

	public TestCaseGeneratorResult getGeneratorResult() {
		return this.generatorResultProperty().get();
	}

	public void setGeneratorResult(TestCaseGeneratorResult generatorResult) {
		this.generatorResultProperty().set(generatorResult);
	}

	public ListProperty<Trace> examplesProperty() {
		return examples;
	}

	public ObservableList<Trace> getExamples() {
		return examples.get();
	}

	@Override
	public Trace getTrace() {
		return this.getExamples().isEmpty() ? null : this.getExamples().get(0);
	}

	@JsonIgnore
	public abstract String getConfigurationDescription();

	@Override
	public void execute(final ExecutionContext context) {
		this.getExamples().clear();

		ConstraintBasedTestCaseGenerator cbTestCaseGenerator = new ConstraintBasedTestCaseGenerator(context.stateSpace(), this.getTestCaseGeneratorSettings(), new ArrayList<>());
		TestCaseGeneratorResult res = cbTestCaseGenerator.generateTestCases();
		this.setGeneratorResult(res);

		List<Trace> traces = new ArrayList<>();
		for (TestTrace trace : res.getTestTraces()) {
			if (trace.getTrace() != null) {
				traces.add(trace.getTrace());
			}
		}

		if (res.isInterrupted()) {
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "animation.testcase.result.interrupted"));
		} else if (traces.isEmpty()) {
			this.setResult(new CheckingResult(CheckingStatus.FAIL, "animation.testcase.result.notFound"));
		} else if (!res.getUncoveredTargets().isEmpty()) {
			this.setResult(new TraceResult(CheckingStatus.FAIL, traces, "animation.testcase.result.notAllGenerated"));
		} else {
			this.setResult(new TraceResult(CheckingStatus.SUCCESS, traces, "animation.testcase.result.found"));
		}
		this.getExamples().addAll(traces);
	}

	@Override
	public void resetAnimatorDependentState() {
		this.setGeneratorResult(null);
		this.examples.clear();
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof TestCaseGenerationItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getMaxDepth(), that.getMaxDepth());
	}
}
