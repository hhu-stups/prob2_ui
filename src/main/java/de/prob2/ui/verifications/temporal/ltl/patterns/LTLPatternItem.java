package de.prob2.ui.verifications.temporal.ltl.patterns;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.temporal.TemporalCheckingResultItem;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class LTLPatternItem {
	// The pattern name is automatically parsed from the code.
	// We store the parsed name in the project file
	// so that we don't need to re-parse the pattern just to get its name
	// every time the project/machine is loaded.
	private final String name;
	private final String description;
	private final String code;

	@JsonIgnore
	final ObjectProperty<TemporalCheckingResultItem> resultItem;
	@JsonIgnore
	final ObjectProperty<CheckingStatus> status;

	@JsonCreator
	public LTLPatternItem(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("code") final String code
	) {
		super();

		this.name = Objects.requireNonNull(name, "name");
		this.description = Objects.requireNonNull(description, "description");
		this.code = Objects.requireNonNull(code, "code");

		this.resultItem = new SimpleObjectProperty<>(this, "resultItem", null);
		this.status = new SimpleObjectProperty<>(this, "status", CheckingStatus.NOT_CHECKED);
		this.resultItemProperty().addListener((o, from, to) -> this.status.set(to == null ? CheckingStatus.NOT_CHECKED : to.getStatus()));
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

	public ObjectProperty<TemporalCheckingResultItem> resultItemProperty() {
		return this.resultItem;
	}

	public TemporalCheckingResultItem getResultItem() {
		return this.resultItemProperty().get();
	}

	public void setResultItem(final TemporalCheckingResultItem resultItem) {
		this.resultItemProperty().set(resultItem);
	}

	public ReadOnlyObjectProperty<CheckingStatus> statusProperty() {
		return this.status;
	}

	public CheckingStatus getStatus() {
		return this.statusProperty().get();
	}

	public void reset() {
		this.setResultItem(null);
	}
}
