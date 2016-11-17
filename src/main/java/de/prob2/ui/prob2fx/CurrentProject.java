package de.prob2.ui.prob2fx;

import java.io.File;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.project.Project;
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
	private final BooleanProperty exists;
	private final BooleanProperty isSingleFile;
	private final ListProperty<File> files;

	@Inject
	private CurrentProject() {
		this.exists = new SimpleBooleanProperty(this, "exists", false);
		this.isSingleFile = new SimpleBooleanProperty(this, "isSingleFile", false);
		this.files = new SimpleListProperty<>(this, "files", FXCollections.observableArrayList());
	}
	
	@Override
    public String getName() {
        return this.get().getName();
    }

	public void changeCurrentProjet(Project project) {
		this.set(project);
	}

	public void addFile(File file) {
		if (!this.isSingleFile()) {
			ObservableList<File> list = this.getFiles();
			list.add(file);
			this.set(new Project(this.getName(), list));
		}
	}

	public ReadOnlyListProperty<File> filesProperty() {
		return this.files;
	}
	
	public ObservableList<File> getFiles() {
		return (ObservableList<File>) this.get().getFiles();
	}
	
	public ReadOnlyBooleanProperty isSingleFileProperty() {
		return this.isSingleFile;
	}

	public boolean isSingleFile() {
		return this.get().isSingleFile();
	}

	public ReadOnlyBooleanProperty existsProperty() {
		return this.exists;
	}

	public boolean exists() {
		return this.get() != null;
	}
}
