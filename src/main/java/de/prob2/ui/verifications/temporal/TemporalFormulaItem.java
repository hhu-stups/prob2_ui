package de.prob2.ui.verifications.temporal;

import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.temporal.ctl.CTLFormulaChecker;
import de.prob2.ui.verifications.temporal.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
	"type",
	"id",
	"description",
	"code",
	"expectedResult",
	"selected",
})
public class TemporalFormulaItem extends AbstractCheckableItem implements IValidationTask {

	public static enum TemporalType {
		LTL, CTL;
	}

	private final TemporalType type;
	private final String id;
	private final String code;
	private final String description;
	private final boolean expectedResult;
	
	@JsonIgnore
	private final ObjectProperty<Trace> counterExample = new SimpleObjectProperty<>(this, "counterExample", null);
	
	@JsonCreator
	public TemporalFormulaItem(
		@JsonProperty("type") final TemporalType type,
		@JsonProperty("id") final String id,
		@JsonProperty("code") final String code,
		@JsonProperty("description") final String description,
		@JsonProperty("expectedResult") final boolean expectedResult
	) {
		super();

		this.type = type;
		this.id = id;
		this.code = code;
		this.description = description;
		this.expectedResult = expectedResult;
	}
	
	@Override
	public void reset() {
		super.reset();
		this.setCounterExample(null);
	}

	public TemporalType getType() {
		return type;
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

	public boolean getExpectedResult() {
		return this.expectedResult;
	}
	
	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate("verifications.ltl.type");
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
	public boolean settingsEqual(final IExecutableItem obj) {
		if (!(obj instanceof TemporalFormulaItem)) {
			return false;
		}
		final TemporalFormulaItem other = (TemporalFormulaItem)obj;
		return this.type == other.type
			&& Objects.equals(this.getId(), other.getId())
			&& this.getCode().equals(other.getCode())
			&& this.getDescription().equals(other.getDescription())
			&& this.expectedResult == other.expectedResult;
	}

	@Override
	@JsonIgnore
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s, %s,%s,%s)", type, this.getClass().getSimpleName(), this.getId(), this.getCode(), this.getExpectedResult());
	}
	
	@Override
	public void execute(final ExecutionContext context) {
		if(type == TemporalType.LTL) {
			LTLFormulaChecker.checkFormula(this, context.getMachine(), context.getStateSpace());
		} else {
			CTLFormulaChecker.checkFormula(this, context.getStateSpace());
		}
	}
}
