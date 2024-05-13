package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.analysis.testcasegeneration.Target;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorResult;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorSettings;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@JsonPropertyOrder({
	"maxDepth",
	"selected",
})
public abstract class TestCaseGenerationItem extends AbstractCheckableItem implements IValidationTask {
	private final int maxDepth;

	@JsonIgnore
	private final ObjectProperty<TestCaseGeneratorResult> result = new SimpleObjectProperty<>(this, "result", null);

	@JsonIgnore
	private final ListProperty<Trace> examples = new SimpleListProperty<>(this, "examples", FXCollections.observableArrayList());

	protected TestCaseGenerationItem(final int maxDepth) {
		super();
		this.maxDepth = maxDepth;
	}

	@JsonIgnore // TODO
	@Override
	public String getId() {
		return null; // TODO
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

	public ObjectProperty<TestCaseGeneratorResult> resultProperty() {
		return this.result;
	}

	public TestCaseGeneratorResult getResult() {
		return this.resultProperty().get();
	}

	public void setResult(final TestCaseGeneratorResult result) {
		this.resultProperty().set(result);
	}

	public ListProperty<Trace> examplesProperty() {
		return examples;
	}

	public ObservableList<Trace> getExamples() {
		return examples.get();
	}

	@JsonIgnore
	protected abstract String getConfigurationDescription();

	public String createdByForMetadata(int index) {
		final Target target = this.getResult().getTestTraces().get(index).getTarget();
		return "Test Case Generation: " + this.getConfigurationDescription() + "; OPERATION: " + target.getOperation() + ", GUARD: " + target.getGuardString();
	}

	@Override
	public void execute(final ExecutionContext context) {
		TestCaseGenerator.generateTestCases(this, context.stateSpace());
	}

	@Override
	public void reset() {
		super.reset();
		this.setResult(null);
		this.examples.clear();
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof TestCaseGenerationItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getMaxDepth(), that.getMaxDepth());
	}
}
