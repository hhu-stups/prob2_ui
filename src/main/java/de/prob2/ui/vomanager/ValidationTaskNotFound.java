package de.prob2.ui.vomanager;

import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Placeholder representing an invalid validation task ID that appeared in a VO expression.
 * This "task" always has an error status.
 */
public final class ValidationTaskNotFound implements IValidationTask {
	private final ReadOnlyObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.INVALID_TASK);
	private final String id;

	public ValidationTaskNotFound(final String id) {
		this.id = id;
	}

	@JsonCreator
	public ValidationTaskNotFound() {
		throw new UnsupportedOperationException("Validation task type INVALID cannot appear in a project file");
	}

	@Override
	public ReadOnlyObjectProperty<Checked> checkedProperty() {
		return this.checked;
	}

	@Override
	public Checked getChecked() {
		return this.checkedProperty().get();
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public ValidationTaskType<ValidationTaskNotFound> getTaskType() {
		return BuiltinValidationTaskTypes.INVALID;
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate("vomanager.validationTaskNotFound.type");
	}

	@Override
	public String getTaskDescription(final I18n i18n) {
		return i18n.translate("vomanager.validationTaskNotFound.description");
	}

	@Override
	public void reset() {
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof ValidationTaskNotFound that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId());
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s)", this.getClass().getSimpleName(), this.getId());
	}
}
