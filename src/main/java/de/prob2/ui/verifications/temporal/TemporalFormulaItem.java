package de.prob2.ui.verifications.temporal;

import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.temporal.ctl.CTLFormulaChecker;
import de.prob2.ui.verifications.temporal.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
	"type",
	"id",
	"description",
	"stateLimit",
	"code",
	"expectedResult",
	"selected",
})
public final class TemporalFormulaItem extends AbstractCheckableItem implements IValidationTask {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;
	private final TemporalFormulaType type;
	private final String code;
	private final String description;
	private final int stateLimit;
	private final boolean expectedResult;

	@JsonIgnore
	private final ObjectProperty<Trace> counterExample = new SimpleObjectProperty<>(this, "counterExample", null);

	@JsonCreator
	public TemporalFormulaItem(
		@JsonProperty("type") final TemporalFormulaType type,
		@JsonProperty("id") final String id,
		@JsonProperty("code") final String code,
		@JsonProperty("description") final String description,
		@JsonProperty("stateLimit") final int stateLimit,
		@JsonProperty("expectedResult") final boolean expectedResult
	) {
		super();

		this.type = type;
		this.id = id;
		this.code = code;
		this.description = description;
		this.stateLimit = stateLimit;
		this.expectedResult = expectedResult;
	}

	public TemporalFormulaType getType() {
		return type;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public ValidationTaskType<TemporalFormulaItem> getTaskType() {
		return BuiltinValidationTaskTypes.TEMPORAL;
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

	public boolean getExpectedResult() {
		return this.expectedResult;
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate(this.getType());
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
	public void execute(final ExecutionContext context) {
		if (type == TemporalFormulaType.LTL) {
			LTLFormulaChecker.checkFormula(this, context.machine(), context.stateSpace());
		} else if (type == TemporalFormulaType.CTL) {
			CTLFormulaChecker.checkFormula(this, context.stateSpace());
		} else {
			throw new AssertionError();
		}
	}

	@Override
	public void reset() {
		super.reset();
		this.setCounterExample(null);
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof TemporalFormulaItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getType(), that.getType())
			       && Objects.equals(this.getCode(), that.getCode())
			       && Objects.equals(this.getDescription(), that.getDescription())
			       && Objects.equals(this.getStateLimit(), that.getStateLimit())
			       && Objects.equals(this.getExpectedResult(), that.getExpectedResult());
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s,%s,%s,%s)", type, this.getClass().getSimpleName(), this.getId(), this.getCode(), this.getExpectedResult());
	}
}
