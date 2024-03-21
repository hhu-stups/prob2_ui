package de.prob2.ui.verifications.temporal.ltl.patterns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.IResettable;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class LTLPatternItem implements IResettable {
	// The pattern name is automatically parsed from the code.
	// We store the parsed name in the project file
	// so that we don't need to re-parse the pattern just to get its name
	// every time the project/machine is loaded.
	private final String name;
	private final String description;
	private final String code;

	@JsonIgnore
	final ObjectProperty<CheckingResultItem> resultItem;
	@JsonIgnore
	final ObjectProperty<Checked> checked;

	@JsonCreator
	public LTLPatternItem(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("code") final String code
	) {
		super();

		this.name = name;
		this.description = description;
		this.code = code;

		this.resultItem = new SimpleObjectProperty<>(this, "resultItem", null);
		this.checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);
		this.resultItemProperty().addListener((o, from, to) -> this.checked.set(to == null ? Checked.NOT_CHECKED : to.getChecked()));
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public String getCode() {
		return this.code;
	}

	public boolean settingsEqual(final LTLPatternItem other) {
		return this.getName().equals(other.getName());
	}

	public ObjectProperty<CheckingResultItem> resultItemProperty() {
		return this.resultItem;
	}

	public CheckingResultItem getResultItem() {
		return this.resultItemProperty().get();
	}

	public void setResultItem(final CheckingResultItem resultItem) {
		this.resultItemProperty().set(resultItem);
	}

	public ReadOnlyObjectProperty<Checked> checkedProperty() {
		return this.checked;
	}

	public Checked getChecked() {
		return this.checkedProperty().get();
	}

	@Override
	public void reset() {
		this.setResultItem(null);
	}
}
