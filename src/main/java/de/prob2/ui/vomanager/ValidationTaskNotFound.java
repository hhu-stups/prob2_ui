package de.prob2.ui.vomanager;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Placeholder representing an invalid validation task ID that appeared in a VO expression.
 * This "task" always has an error status.
 */
public final class ValidationTaskNotFound implements IValidationTask {
	private final ReadOnlyObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.PARSE_ERROR);
	private final String id;
	
	public ValidationTaskNotFound(final String id) {
		this.id = id;
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
	public String getTaskDescription(final I18n i18n) {
		return i18n.translate("vomanager.validationTaskNotFound");
	}

	@Override
	@JsonIgnore
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s)", this.getClass().getSimpleName(), this.getId());
	}
}
