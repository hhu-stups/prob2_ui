package de.prob2.ui.animation.tracereplay;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.PostconditionPredicate;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.TraceViewHandler;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TraceSaver {

	private final Injector injector;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceSaver.class);

	@Inject
	public TraceSaver(final Injector injector, final CurrentTrace currentTrace, final CurrentProject currentProject) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
	}

	public Path saveTrace(Window window, TraceReplayErrorAlert.Trigger trigger) {
		return saveTrace(window, false, trigger);
	}

	public Path saveTrace(Window window, boolean recordTests, TraceReplayErrorAlert.Trigger trigger) {
		TraceFileHandler traceSaver = injector.getInstance(TraceFileHandler.class);
		Trace possiblyLostTrace = currentTrace.get();
		if (currentTrace.get() != null) {
			try {
				return traceSaver.save(possiblyLostTrace, currentProject.getCurrentMachine(), recordTests);
			} catch (Exception e) {
				LOGGER.error("", e);
				TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, "traceSave.buttons.saveTrace.error.msg", trigger, Collections.EMPTY_LIST);
				alert.initOwner(window);
				alert.setAttemptedReplayOrLostTrace(possiblyLostTrace);
				alert.setErrorMessage();
			}
		}
		return null;
	}

	public void saveTraceAndAddTests(Window window, TraceReplayErrorAlert.Trigger trigger) {
		Path path = this.saveTrace(window, trigger);
		if(path != null) {
			Path relativizedPath = currentProject.getLocation().relativize(path);
			ReplayTrace replayTrace = injector.getInstance(TraceViewHandler.class).getMachinesToTraces().get(currentProject.getCurrentMachine()).get().stream()
					.filter(t -> t.getLocation().equals(relativizedPath))
					.collect(Collectors.toList())
					.get(0);
			TraceTestView traceTestView = injector.getInstance(TraceTestView.class);
			traceTestView.loadReplayTrace(replayTrace);
			traceTestView.show();
		}
	}

	public void saveTraceAndRecordTests(Window window, TraceReplayErrorAlert.Trigger trigger) {
		this.saveTrace(window, true, trigger);
	}

}
