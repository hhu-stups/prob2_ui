package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.base.MoreObjects;
import com.google.common.io.MoreFiles;

import de.prob.scripting.FactoryProvider;
import de.prob.scripting.ModelFactory;
import de.prob2.ui.internal.CachedEditorState;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.verifications.Checked;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@JsonPropertyOrder({
	"name",
	"description",
	"location",
	"lastUsedPreferenceName",
	"machineProperties"
})
public final class Machine {

	private final StringProperty name;
	private final StringProperty description;
	private final Path location;
	private final StringProperty lastUsedPreferenceName;
	@JsonUnwrapped
	private final MachineProperties machineProperties;

	@JsonIgnore
	private final CachedEditorState cachedEditorState;
	@JsonIgnore
	private final BooleanProperty changed = new SimpleBooleanProperty(false);

	public Machine(final String name, final String description, final Path location) {
		this(name, description, location, null);
	}

	@JsonCreator
	public Machine(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("location") final Path location,
		@JsonProperty("lastUsedPreferenceName") final String lastUsedPreferenceName
	) {
		this.name = new SimpleStringProperty(this, "name", Objects.requireNonNull(name, "name"));
		this.description = new SimpleStringProperty(this, "description", Objects.requireNonNull(description, "description"));
		this.location = Objects.requireNonNull(location, "location");
		this.lastUsedPreferenceName = new SimpleStringProperty(this, "lastUsedPreferenceName", lastUsedPreferenceName != null && !lastUsedPreferenceName.isEmpty() ? lastUsedPreferenceName : Preference.DEFAULT.getName());
		// combining @JsonCreator and @JsonUnwrapped is not supported (yet)
		// let's hope that Jackson uses getters/setters for that property
		// https://github.com/FasterXML/jackson-databind/issues/1467
		// https://github.com/FasterXML/jackson-databind/issues/1497
		// https://github.com/FasterXML/jackson-databind/issues/3726
		// https://github.com/FasterXML/jackson-databind/issues/3754
		// https://github.com/FasterXML/jackson-databind/pull/4271
		this.machineProperties = new MachineProperties();

		this.cachedEditorState = new CachedEditorState();

		this.initListeners();
	}

	private void initListeners() {
		final InvalidationListener changedListener = o -> this.setChanged(true);
		this.nameProperty().addListener(changedListener);
		this.descriptionProperty().addListener(changedListener);
		this.lastUsedPreferenceNameProperty().addListener(changedListener);

		this.getMachineProperties().changedProperty().addListener(((observable, from, to) -> {
			if (to != null && to) {
				this.setChanged(true);
			}
		}));
	}

	// these getters might be required for the UI (via reflection)
	@JsonIgnore
	public Checked getChecked() {
		return null;
	}

	public ObjectProperty<Checked> checkedProperty() {
		return null;
	}

	@JsonIgnore
	public Class<? extends ModelFactory<?>> getModelFactoryClass() {
		return FactoryProvider.factoryClassFromExtension(MoreFiles.getFileExtension(this.getLocation()));
	}

	public StringProperty lastUsedPreferenceNameProperty() {
		return this.lastUsedPreferenceName;
	}

	public String getLastUsedPreferenceName() {
		return this.lastUsedPreferenceNameProperty().get();
	}

	public void setLastUsedPreferenceName(final String lastUsedPreferenceName) {
		this.lastUsedPreferenceNameProperty().set(lastUsedPreferenceName);
	}

	@JsonUnwrapped
	public MachineProperties getMachineProperties() {
		return this.machineProperties;
	}

	public StringProperty nameProperty() {
		return this.name;
	}

	public String getName() {
		return this.nameProperty().get();
	}

	public void setName(final String name) {
		this.nameProperty().set(name);
	}

	public StringProperty descriptionProperty() {
		return this.description;
	}

	public String getDescription() {
		return this.descriptionProperty().get();
	}

	public void setDescription(final String description) {
		this.descriptionProperty().set(description);
	}

	public Path getLocation() {
		return this.location;
	}

	@JsonIgnore
	public CachedEditorState getCachedEditorState() {
		return cachedEditorState;
	}

	public BooleanProperty changedProperty() {
		return changed;
	}

	@JsonIgnore
	public boolean isChanged() {
		return this.changedProperty().get();
	}

	@JsonIgnore
	public void setChanged(final boolean changed) {
		this.changedProperty().set(changed);
	}

	public void resetChanged() {
		this.getMachineProperties().setChanged(false);
		this.setChanged(false);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		} else if (!(other instanceof Machine otherMachine)) {
			return false;
		} else {
			return this.getLocation().equals(otherMachine.getLocation());
		}
	}

	@Override
	public int hashCode() {
		return this.getLocation().hashCode();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("name", this.getName())
			       .add("location", this.getLocation())
			       .toString();
	}
}
