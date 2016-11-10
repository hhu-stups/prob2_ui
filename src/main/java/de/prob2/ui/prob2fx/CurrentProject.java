package de.prob2.ui.prob2fx;

import com.google.inject.Singleton;

import de.prob2.ui.project.Project;
import javafx.beans.property.SimpleObjectProperty;

@Singleton
public class CurrentProject extends SimpleObjectProperty<Project> {
	public void changeCurrentProjet(Project project) {
		this.set(project);
	}
}
