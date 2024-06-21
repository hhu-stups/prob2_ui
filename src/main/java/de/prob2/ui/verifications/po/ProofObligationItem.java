package de.prob2.ui.verifications.po;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import de.prob.model.eventb.ProofObligation;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class ProofObligationItem implements IValidationTask {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;
	private final String name;

	@JsonIgnore
	private final StringProperty description;
	@JsonIgnore
	private final ObjectProperty<CheckingStatus> status;

	@JsonCreator
	public ProofObligationItem(@JsonProperty("id") String id, @JsonProperty("name") String name) {
		this.id = id;
		this.name = Objects.requireNonNull(name, "name");

		this.description = new SimpleStringProperty(this, "description", null);
		this.status = new SimpleObjectProperty<>(this, "status", CheckingStatus.INVALID_TASK);
	}

	public ProofObligationItem(ProofObligation proofObligation) {
		this(null, proofObligation.getName());
		this.setDescription(proofObligation.getDescription());
		this.setStatus(proofObligation.isDischarged() ? CheckingStatus.SUCCESS : CheckingStatus.NOT_CHECKED);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ValidationTaskType<ProofObligationItem> getTaskType() {
		return BuiltinValidationTaskTypes.PROOF_OBLIGATION;
	}

	public ProofObligationItem withId(final String id) {
		final ProofObligationItem updatedPO = new ProofObligationItem(id, this.getName());
		updatedPO.setDescription(this.getDescription());
		updatedPO.setStatus(this.getStatus());
		return updatedPO;
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate("verifications.po.type");
	}

	@Override
	public String getTaskDescription(I18n i18n) {
		return this.getName();
	}

	public String getName() {
		return this.name;
	}

	public StringProperty descriptionProperty() {
		return this.description;
	}

	@JsonIgnore
	public String getDescription() {
		return this.descriptionProperty().get();
	}

	public void setDescription(String description) {
		this.descriptionProperty().set(description);
	}

	@Override
	public ObjectProperty<CheckingStatus> statusProperty() {
		return this.status;
	}

	@Override
	public CheckingStatus getStatus() {
		return this.statusProperty().get();
	}

	@JsonIgnore
	public void setStatus(final CheckingStatus status) {
		this.statusProperty().set(status);
	}

	@Override
	public CompletableFuture<?> execute(CheckingExecutors executors, ExecutionContext context) {
		// Nothing to be done here - the proof status is loaded when the model is loaded
		// and can only change by reloading the model.
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void resetAnimatorDependentState() {}

	@Override
	public void reset() {
		this.setDescription(null);
		this.setStatus(CheckingStatus.NOT_CHECKED);
		this.resetAnimatorDependentState();
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof ProofObligationItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getName(), that.getName());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("id", this.getId())
			       .add("name", this.getName())
			       .toString();
	}
}
