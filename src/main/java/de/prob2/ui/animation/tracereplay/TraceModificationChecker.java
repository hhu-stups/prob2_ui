package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;
import de.prob.animator.ReusableAnimator;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.check.TraceChecker;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.OperationInfo;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.statusbar.StatusBar;

import java.io.IOException;
import java.util.Map;

public class TraceModificationChecker {

	TraceChecker traceChecker;
	final StageManager stageManager;

	public TraceModificationChecker(PersistentTrace trace, Map<String, OperationInfo> oldInfos, Map<String, OperationInfo> newInfos,
									String newPath, String oldPath, Injector injector, StageManager stageManager) throws IOException, ModelTranslationError {
		traceChecker = new TraceChecker(trace, oldInfos, newInfos, oldPath, newPath, injector);
		this.stageManager = stageManager;
	}


	void makeReport(){
		traceChecker.getDeltaFinder();
	}
}
