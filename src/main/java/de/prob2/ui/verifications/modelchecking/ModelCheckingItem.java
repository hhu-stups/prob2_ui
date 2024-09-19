package de.prob2.ui.verifications.modelchecking;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ICliTask;
import de.prob2.ui.verifications.ISelectableTask;
import de.prob2.ui.verifications.ITraceTask;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

public abstract class ModelCheckingItem implements ICliTask, ISelectableTask, ITraceTask {
	@JsonIgnore
	private final ObjectProperty<CheckingStatus> status = new SimpleObjectProperty<>(this, "status", CheckingStatus.NOT_CHECKED);

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;

	private final BooleanProperty selected;

	@JsonIgnore
	private final ListProperty<ModelCheckingStep> steps = new SimpleListProperty<>(this, "steps", FXCollections.observableArrayList());

	@JsonIgnore
	private final ObjectProperty<ModelCheckingStep> runningStep = new SimpleObjectProperty<>(this, "runningStep", null);

	public ModelCheckingItem(final String id) {
		this.id = id;
		this.selected = new SimpleBooleanProperty(true);
		this.initListeners();
	}

	private void initListeners() {
		this.stepsProperty().addListener((o, from, to) -> {
			if (to.isEmpty()) {
				this.status.set(CheckingStatus.NOT_CHECKED);
			} else {
				boolean inProgress = to.stream()
					.map(ModelCheckingStep::getStatus)
					.anyMatch(CheckingStatus.IN_PROGRESS::equals);
				final boolean failed = !inProgress && to.stream()
					                       .map(ModelCheckingStep::getStatus)
					                       .anyMatch(CheckingStatus.FAIL::equals);
				final boolean success = !failed && to.stream()
					                                   .map(ModelCheckingStep::getStatus)
					                                   .anyMatch(CheckingStatus.SUCCESS::equals);

				if (inProgress) {
					this.status.set(CheckingStatus.IN_PROGRESS);
				} else if (success) {
					this.status.set(CheckingStatus.SUCCESS);
				} else if (failed) {
					this.status.set(CheckingStatus.FAIL);
				} else {
					this.status.set(CheckingStatus.TIMEOUT);
				}
			}
		});
	}

	@Override
	public ReadOnlyObjectProperty<CheckingStatus> statusProperty() {
		return this.status;
	}

	@Override
	public CheckingStatus getStatus() {
		return this.statusProperty().get();
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	@JsonProperty("selected")
	public void setSelected(boolean selected) {
		this.selected.set(selected);
	}

	@JsonProperty("selected")
	@Override
	public boolean selected() {
		return selected.get();
	}

	@Override
	public BooleanProperty selectedProperty() {
		return selected;
	}

	public ListProperty<ModelCheckingStep> stepsProperty() {
		return steps;
	}

	public List<ModelCheckingStep> getSteps() {
		return steps.get();
	}

	public ObjectProperty<ModelCheckingStep> runningStepProperty() {
		return this.runningStep;
	}

	public ModelCheckingStep getRunningStep() {
		return this.runningStepProperty().get();
	}

	public void setRunningStep(final ModelCheckingStep runningStep) {
		this.runningStepProperty().set(runningStep);
	}

	@Override
	public void resetAnimatorDependentState() {
		// Clearing the steps list causes the listener to reset the status,
		// but in this case we want to preserve the status.
		CheckingStatus savedChecked = this.getStatus();
		this.stepsProperty().clear();
		this.status.set(savedChecked);
		this.runningStep.set(null);
	}

	@Override
	public void reset() {
		this.status.set(CheckingStatus.NOT_CHECKED);
		this.resetAnimatorDependentState();
	}
}
