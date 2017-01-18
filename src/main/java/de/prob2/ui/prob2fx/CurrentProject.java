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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.project.Machine;
import de.prob2.ui.project.Project;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

@Singleton
public final class CurrentProject extends SimpleObjectProperty<Project> {
	private static final Charset PROJECT_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(CurrentProject.class);
	
	private final BooleanProperty exists;
	private final BooleanProperty isSingleFile;
	private final ListProperty<Machine> machines;
	private final ObjectProperty<Path> defaultLocation;
	private final Gson gson;

	@Inject
	private CurrentProject() {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.exists = new SimpleBooleanProperty(this, "exists", false);
		this.exists.bind(Bindings.isNotNull(this));
		this.isSingleFile = new SimpleBooleanProperty(this, "isSingleFile", false);
		this.machines = new SimpleListProperty<>(this, "machines", FXCollections.observableArrayList());
		this.defaultLocation = new SimpleObjectProperty<>(this, "defaultLocation", Paths.get(System.getProperty("user.home")));
	}

	@Override
	public void set(Project project) {
		super.set(project);
		this.isSingleFile.set(project.isSingleFile());
	}

	@Override
	public String getName() {
		return this.get().getName();
	}

	public void changeCurrentProject(Project project) {
		this.set(project);
	}

	public void addMachine(File machine) {
		if (!this.isSingleFile()) {
			List<Machine> machinesList = this.getFiles();
			machinesList.add(new Machine(machine.getName().split("\\.")[0], "", machine));
			this.set(new Project(this.getName(), this.get().getDescription(), machinesList, this.get().getPreferences(),
					this.get().getLocation()));
		}
	}

	public ReadOnlyListProperty<Machine> filesProperty() {
		return this.machines;
	}

	public List<Machine> getFiles() {
		return this.get().getMachines();
	}

	public ReadOnlyBooleanProperty isSingleFileProperty() {
		return this.isSingleFile;
	}

	public boolean isSingleFile() {
		return this.isSingleFileProperty().get();
	}

	public ReadOnlyBooleanProperty existsProperty() {
		return this.exists;
	}

	public boolean exists() {
		return this.existsProperty().get();
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

	public void remove() {
		super.set(null);
		this.isSingleFile.set(false);
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
}
