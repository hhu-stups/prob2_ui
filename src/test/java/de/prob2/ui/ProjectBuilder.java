package de.prob2.ui;

import com.google.inject.Injector;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.project.preferences.Preference;

import java.nio.file.Paths;

public class ProjectBuilder {

	private final ProjectManager projectManager;
	private final CurrentProject currentProject;
	private Long time = 3000L;

	public ProjectBuilder(Injector injector){
		this.projectManager = injector.getInstance(ProjectManager.class);
		this.currentProject = injector.getInstance(CurrentProject.class);
	}

	public ProjectBuilder fromFile(String filepath){
		projectManager.openAutomaticProjectFromMachine(Paths.get(filepath));
		return this;
	}

	public ProjectBuilder withAnimatedMachine(String machineName){
		currentProject.startAnimation(currentProject.get().getMachine(machineName), Preference.DEFAULT);
		return this;
	}

	public ProjectBuilder withTrace(String traceFile){
		//TODO
		return this;
	}

	public ProjectBuilder atStateSpace(String traceFile){
//		if(this.trace != null){
//		TODO
//		}
		return this;
	}

	public ProjectBuilder withCustomizedSleep(Long timeInMillisec){
		this.time = timeInMillisec;
		return this;
	}

	public CurrentProject build() throws InterruptedException {
		Thread.sleep(time);
		return this.currentProject;
	}



}
