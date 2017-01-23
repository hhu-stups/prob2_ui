package de.prob2.ui.prob2fx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.project.Machine;
import de.prob2.ui.project.Preference;
import de.prob2.ui.project.Project;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

@Singleton
public final class CurrentProject extends SimpleObjectProperty<Project> {
	private static final Charset PROJECT_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(CurrentProject.class);
	
	private final Gson gson;
	
	private final BooleanProperty exists;
	private final BooleanProperty isSingleFile;
	private final StringProperty name;
	private final StringProperty description;
	private final ListProperty<Machine> machines;
	private final MapProperty<String, Preference> preferences;
	private final ObjectProperty<File> location;
	
	private final ObjectProperty<Path> defaultLocation;

	@Inject
	private CurrentProject() {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.defaultLocation = new SimpleObjectProperty<>(this, "defaultLocation", Paths.get(System.getProperty("user.home")));
		this.exists = new SimpleBooleanProperty(this, "exists", false);
		this.exists.bind(Bindings.isNotNull(this));
		this.isSingleFile = new SimpleBooleanProperty(this, "isSingleFile", true);
		
		this.name = new SimpleStringProperty(this, "name", "");
		this.description = new SimpleStringProperty(this, "description", "");
		this.machines = new SimpleListProperty<>(this, "machines", FXCollections.observableArrayList());
		this.preferences = new SimpleMapProperty<>(this, "preferences", FXCollections.observableHashMap());
		this.location = new SimpleObjectProperty<>(this,"location", null);
		
		this.addListener((observable, from, to) -> {
			if(to == null) {
				this.name.set("");
				this.description.set("");
				this.machines.clear();
				this.preferences.clear();
				this.location.set(null);
				this.isSingleFile.set(true);
			} else {
				this.name.set(to.getName());
				this.description.set(to.getDescription());
				this.machines.setAll(to.getMachines());
				this.preferences.putAll(to.getPreferences());
				this.location.set(to.getLocation());
				this.isSingleFile.set(to.isSingleFile());
			}
		}); 
	}

	public void addMachine(File machine) {
		if (!this.isSingleFile()) {
			List<Machine> machinesList = this.getMachines();
			machinesList.add(new Machine(machine.getName().split("\\.")[0], "", machine));
			this.set(new Project(this.getName(), this.get().getDescription(), machinesList, this.get().getPreferences(),
					this.get().getLocation()));
		}
	}
	
	public void remove() {
		super.set(null);
	}
	
	public ReadOnlyBooleanProperty existsProperty() {
		return this.exists;
	}

	public boolean exists() {
		return this.existsProperty().get();
	}
	
	public StringProperty nameProperty() {
		return this.name;
	}
	
	@Override
	public String getName() {
		return this.nameProperty().get();
	}
	
	public StringProperty descriptionProperty() {
		return this.description;
	}
	
	public String getDescription() {
		return this.descriptionProperty().get();
	}

	public ReadOnlyListProperty<Machine> machinesProperty() {
		return this.machines;
	}

	public List<Machine> getMachines() {
		return this.get().getMachines();
	}
	
	public ReadOnlyMapProperty<String, Preference> preferencesProperty() {
		return this.preferences;
	}
	
	public Map<String, Preference> getPreferences() {
		return this.preferencesProperty().get();
	}
	
	public ObjectProperty<File> locationProperty() {
		return this.location;
	}
	
	public File getLocation() {
		return this.locationProperty().get();
	}

	public ReadOnlyBooleanProperty isSingleFileProperty() {
		return this.isSingleFile;
	}

	public boolean isSingleFile() {
		return this.isSingleFileProperty().get();
	}

	public ObjectProperty<Path> defaultLocationProperty() {
		return this.defaultLocation;
	}
	
	public Path getDefaultLocation() {
		return this.defaultLocationProperty().get();
	}

	public void setDefaultLocation(Path defaultProjectLocation) {
		this.defaultLocationProperty().set(defaultProjectLocation);
	}
	
	public void save() {
		File location = new File(this.get().getLocation() + File.separator + this.getName() + ".json");
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(location), PROJECT_CHARSET)) {
			gson.toJson(this.get(), writer);
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Failed to create project data file", exc);
		} catch (IOException exc) {
			LOGGER.warn("Failed to save project", exc);
		}
	}

	public void open(File file) {
		Project project;
		try (final Reader reader = new InputStreamReader(new FileInputStream(file), PROJECT_CHARSET)) {
			project = gson.fromJson(reader, Project.class);
			project.setLocation(file.getParentFile());
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Project file not found", exc);
			return;
		} catch (IOException exc) {
			LOGGER.warn("Failed to open project file", exc);
			return;
		}
		this.set(project);
	}
}
