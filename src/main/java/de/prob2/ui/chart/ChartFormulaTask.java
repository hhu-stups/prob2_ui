package de.prob2.ui.chart;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.IFormulaTask;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@JsonPropertyOrder({
	"id",
	"formula",
})
public final class ChartFormulaTask implements IFormulaTask {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;
	private final String formula;
	@JsonIgnore
	private final ObjectProperty<CheckingStatus> status;

	@JsonCreator
	public ChartFormulaTask(
			@JsonProperty("id") final String id,
			@JsonProperty("formula") final String formula
	) {
		this.id = id;
		this.formula = Objects.requireNonNull(formula, "formula");
		this.status = new SimpleObjectProperty<>(this, "status", CheckingStatus.NOT_CHECKED);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ValidationTaskType<ChartFormulaTask> getTaskType() {
		return BuiltinValidationTaskTypes.CHART_FORMULA;
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate("chart.historyChart.task.name");
	}

	@Override
	public String getTaskDescription(I18n i18n) {
		return this.getFormula();
	}

	@Override
	public String getFormula() {
		return this.formula;
	}

	@Override
	public ObjectProperty<CheckingStatus> statusProperty() {
		return this.status;
	}

	@Override
	public CheckingStatus getStatus() {
		return this.statusProperty().get();
	}

	public void setStatus(final CheckingStatus status) {
		this.statusProperty().set(status);
	}

	@Override
	public CompletableFuture<?> execute(CheckingExecutors executors, ExecutionContext context) {
		// TODO Do the visualization, then ask the user to decide if it is correct or not.
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void resetAnimatorDependentState() {}

	@Override
	public void reset() {
		this.setStatus(CheckingStatus.NOT_CHECKED);
		this.resetAnimatorDependentState();
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof ChartFormulaTask that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getFormula(), that.getFormula());
	}

	@Override
	public ChartFormulaTask copy() {
		return new ChartFormulaTask(this.id, this.formula);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("id", this.getId())
			       .add("formula", this.getFormula())
			       .toString();
	}
}
