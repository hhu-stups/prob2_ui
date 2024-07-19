package de.prob2.ui.verifications.modelchecking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.*;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;
import javafx.beans.property.*;
import javafx.collections.FXCollections;

import java.util.*;

// TODO: adapt JSON handling
public abstract class ModelCheckingItem implements ICliTask, ISelectableTask, ITraceTask {
	@JsonIgnore
	private final ObjectProperty<CheckingStatus> status = new SimpleObjectProperty<>(this, "status", CheckingStatus.NOT_CHECKED);

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;

	private final BooleanProperty selected;

	@JsonIgnore
	private final ListProperty<ModelCheckingStep> steps = new SimpleListProperty<>(this, "steps", FXCollections.observableArrayList());

	@JsonIgnore
	private final ObjectProperty<ModelCheckingStep> currentStep = new SimpleObjectProperty<>(this, "currentStep", null);

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
				final boolean failed = to.stream()
					                       .map(ModelCheckingStep::getStatus)
					                       .anyMatch(CheckingStatus.FAIL::equals);
				final boolean success = !failed && to.stream()
					                                   .map(ModelCheckingStep::getStatus)
					                                   .anyMatch(CheckingStatus.SUCCESS::equals);

				if (success) {
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
	public ValidationTaskType<ModelCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.MODEL_CHECKING;
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate("verifications.modelchecking.type");
	}

	public abstract String getTaskDescription(final I18n i18n);

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

	public ObjectProperty<ModelCheckingStep> currentStepProperty() {
		return this.currentStep;
	}

	public ModelCheckingStep getCurrentStep() {
		return this.currentStepProperty().get();
	}

	public void setCurrentStep(final ModelCheckingStep currentStep) {
		this.currentStepProperty().set(currentStep);
	}

	public abstract Trace getTrace();

	public abstract void execute(final ExecutionContext context);

	@Override
	public void resetAnimatorDependentState() {
		// Clearing the steps list causes the listener to reset the status,
		// but in this case we want to preserve the status.
		CheckingStatus savedChecked = this.getStatus();
		this.stepsProperty().clear();
		this.status.set(savedChecked);
		this.currentStep.set(null);
	}

	@Override
	public void reset() {
		this.status.set(CheckingStatus.NOT_CHECKED);
		this.resetAnimatorDependentState();
	}

	@Override
	public boolean settingsEqual(Object obj) {
		boolean equals = false;
		if (obj instanceof ProBModelCheckingItem proBItem) {
			equals = proBItem.settingsEqual(this);
		} else if (obj instanceof TLCModelCheckingItem tlcItem) {
			equals = tlcItem.settingsEqual(this);
		}
		return equals;
	}
}
