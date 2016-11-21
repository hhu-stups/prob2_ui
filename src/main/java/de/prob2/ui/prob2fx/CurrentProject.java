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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.project.Project;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Singleton
public class CurrentProject extends SimpleObjectProperty<Project> {
	private static final Charset CONFIG_CHARSET = Charset.forName("UTF-8");
	private static final Logger logger = LoggerFactory.getLogger(CurrentProject.class);
	private final BooleanProperty exists;
	private final BooleanProperty isSingleFile;
	private final ListProperty<File> files;
	private final Gson gson;

	@Inject
	private CurrentProject() {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.exists = new SimpleBooleanProperty(this, "exists", false);
		this.exists.bind(Bindings.isNotNull(this));
		this.isSingleFile = new SimpleBooleanProperty(this, "isSingleFile", false);
		this.files = new SimpleListProperty<>(this, "files", FXCollections.observableArrayList());
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

	public void addFile(File file) {
		if (!this.isSingleFile()) {
			ObservableList<File> list = this.getFiles();
			list.add(file);
			this.set(new Project(this.getName(), list, this.get().getLocation()));
		}
	}

	public ReadOnlyListProperty<File> filesProperty() {
		return this.files;
	}

	public ObservableList<File> getFiles() {
		return (ObservableList<File>) this.get().getMachines();
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
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(location), CONFIG_CHARSET)) {
			gson.toJson(this.get(), writer);
		} catch (FileNotFoundException exc) {
			logger.warn("Failed to create project data file", exc);
		} catch (IOException exc) {
			logger.warn("Failed to save project", exc);
		}
	}

	public void open(File file) {
		Project project;
		try (final Reader reader = new InputStreamReader(new FileInputStream(file), CONFIG_CHARSET)) {
			project = gson.fromJson(reader, Project.class);
		} catch (FileNotFoundException exc) {
			logger.warn("Project file not found", exc);
			return;
		} catch (IOException exc) {
			logger.warn("Failed to open project file", exc);
			return;
		}
		this.set(project);
	}
}
