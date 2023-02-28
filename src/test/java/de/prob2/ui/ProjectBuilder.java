package de.prob2.ui;

import com.google.inject.Injector;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProjectBuilder {

	private final ProjectManager projectManager;
	private final CurrentProject currentProject;
	private ReplayTrace replayTrace = null;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private Long time = 3000L;
	private String machineName;
	private boolean isAnimated = false;

	public ProjectBuilder(Injector injector) {
		this.projectManager = injector.getInstance(ProjectManager.class);
		this.currentProject = injector.getInstance(CurrentProject.class);
		this.currentTrace = injector.getInstance(CurrentTrace.class);
		this.injector = injector;
	}

	public ProjectBuilder fromFile(String filepath) {
		projectManager.openAutomaticProjectFromMachine(Paths.get(filepath));
		return this;
	}

	public ProjectBuilder withAnimatedMachine(String machineName) {
		this.machineName = machineName;
		this.isAnimated = true;
		return this;
	}


	// TODO: currently the added replayTrace is just related to the machine but cannot be loaded
	public ProjectBuilder withReplayedTrace(String traceFile) {
		Path tracePath = Paths.get(traceFile);
		this.replayTrace = new ReplayTrace(null, tracePath, tracePath.toAbsolutePath(), injector.getInstance(TraceManager.class));
		List<ReplayTrace> replayTraces = new ArrayList<>();
		replayTraces.add(replayTrace);
		currentProject.get().getMachine(machineName).tracesProperty().setAll(replayTraces);
//		TraceChecker traceChecker = injector.getInstance(TraceChecker.class);
//		traceChecker.check(replayTrace);
		return this;
	}


//		TODO
//		public ProjectBuilder atStateSpace(String traceFile){
//		if(this.trace != null){
//
//		}
//		return this;
// }

	public ProjectBuilder withCustomizedSleep(Long timeInMillisec){
		this.time = timeInMillisec;
		return this;
	}

	public ProjectBuilder withLTLFormula(LTLFormulaItem ltlFormulaItem) {
		currentProject.getCurrentMachine().ltlFormulasProperty().add(ltlFormulaItem);
		return this;
	}

	public CurrentProject build() throws InterruptedException {
		if (isAnimated) currentProject.startAnimation(currentProject.get().getMachine(machineName), Preference.DEFAULT);
		Thread.sleep(time);
		return this.currentProject;
	}

}
