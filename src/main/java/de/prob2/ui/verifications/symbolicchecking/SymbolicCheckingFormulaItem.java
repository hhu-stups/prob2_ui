package de.prob2.ui.verifications.symbolicchecking;

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
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@JsonPropertyOrder({
	"type",
	"code",
	"selected",
})
public final class SymbolicCheckingFormulaItem extends AbstractCheckableItem implements IValidationTask<SymbolicCheckingFormulaItem> {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;
	private final SymbolicCheckingType type;
	private final String code;
	
	@JsonIgnore
	private final ListProperty<Trace> counterExamples = new SimpleListProperty<>(this, "counterExamples", FXCollections.observableArrayList());
	
	@JsonCreator
	public SymbolicCheckingFormulaItem(
		@JsonProperty("id") final String id,
		@JsonProperty("code") final String code,
		@JsonProperty("type") final SymbolicCheckingType type
	) {
		super();
		this.id = id;
		this.type = type;
		this.code = code;
	}
	
	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public ValidationTaskType<SymbolicCheckingFormulaItem> getTaskType() {
		return BuiltinValidationTaskTypes.SYMBOLIC;
	}

	public SymbolicCheckingType getType() {
		return this.type;
	}
	
	public String getCode() {
		return this.code;
	}
	
	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate(this.getType());
	}
	
	@Override
	public String getTaskDescription(final I18n i18n) {
		return this.getCode();
	}
	
	public ObservableList<Trace> getCounterExamples() {
		return counterExamples.get();
	}
	
	public ListProperty<Trace> counterExamplesProperty() {
		return counterExamples;
	}
	
	@Override
	public boolean settingsEqual(final IExecutableItem obj) {
		if (!(obj instanceof SymbolicCheckingFormulaItem other)) {
			return false;
		}
		return Objects.equals(this.getId(), other.getId())
			&& this.getType().equals(other.getType())
			&& this.getCode().equals(other.getCode());
	}
	
	@Override
	public void reset() {
		super.reset();
		this.counterExamples.clear();
	}

	@Override
	@JsonIgnore
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s,%s)", this.getClass().getSimpleName(), this.getId(), this.getCode());
	}
	
	@Override
	public void execute(final ExecutionContext context) {
		SymbolicCheckingFormulaHandler.checkItem(this, context.stateSpace());
	}
}
