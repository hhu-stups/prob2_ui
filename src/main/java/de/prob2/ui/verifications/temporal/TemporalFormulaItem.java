package de.prob2.ui.verifications.temporal;

import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ICliTask;
import de.prob2.ui.verifications.ITraceTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
	"id",
	"description",
	"stateLimit",
	"code",
	"startState",
	"expectedResult",
	"selected",
})
public abstract class TemporalFormulaItem extends AbstractCheckableItem implements ICliTask, ITraceTask {
	public enum StartState {
		ALL_INITIAL_STATES,
		CURRENT_STATE,
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;
	private final String code;
	private final String description;
	private final int stateLimit;
	private final TemporalFormulaItem.StartState startState;
	private final boolean expectedResult;

	@JsonIgnore
	private final ObjectProperty<Trace> counterExample = new SimpleObjectProperty<>(this, "counterExample", null);

	protected TemporalFormulaItem(String id, String code, String description, int stateLimit, TemporalFormulaItem.StartState startState, boolean expectedResult) {
		super();

		this.id = id;
		this.code = Objects.requireNonNull(code, "code");
		this.description = Objects.requireNonNull(description, "description");
		this.stateLimit = stateLimit;
		this.startState = Objects.requireNonNull(startState, "startState");
		this.expectedResult = expectedResult;
	}

	@Override
	public String getId() {
		return this.id;
	}

	public String getCode() {
		return this.code;
	}

	public String getDescription() {
		return this.description;
	}

	public int getStateLimit() {
		return this.stateLimit;
	}

	public TemporalFormulaItem.StartState getStartState() {
		return this.startState;
	}

	public boolean getExpectedResult() {
		return this.expectedResult;
	}

	@Override
	public String getTaskDescription(final I18n i18n) {
		if (this.getDescription().isEmpty()) {
			return this.getCode();
		} else {
			return this.getCode() + " // " + getDescription();
		}
	}

	public void setCounterExample(Trace counterExample) {
		this.counterExample.set(counterExample);
	}

	public Trace getCounterExample() {
		return counterExample.get();
	}

	public ObjectProperty<Trace> counterExampleProperty() {
		return counterExample;
	}

	@Override
	public Trace getTrace() {
		return this.getCounterExample();
	}

	@Override
	public void resetAnimatorDependentState() {
		this.setCounterExample(null);
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof TemporalFormulaItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getCode(), that.getCode())
			       && Objects.equals(this.getDescription(), that.getDescription())
			       && Objects.equals(this.getStateLimit(), that.getStateLimit())
			       && this.getStartState().equals(that.getStartState())
			       && Objects.equals(this.getExpectedResult(), that.getExpectedResult());
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s,%s,%s)", this.getClass().getSimpleName(), this.getId(), this.getCode(), this.getExpectedResult());
	}
}
