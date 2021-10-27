package de.prob2.ui.animation.tracereplay;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.TraceViewHandler;
import javafx.scene.control.Alert;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collections;
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
		TraceFileHandler traceSaver = injector.getInstance(TraceFileHandler.class);
		Trace possiblyLostTrace = currentTrace.get();
		if (currentTrace.get() != null) {
			try {
				return traceSaver.save(possiblyLostTrace, currentProject.getCurrentMachine());
			} catch (Exception e) {
				LOGGER.error("", e);
				TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, "traceSave.buttons.saveTrace.error.msg", trigger, Collections.EMPTY_LIST);
				alert.initOwner(window);
				alert.setAttemptedReplayOrLostTrace(possiblyLostTrace);
				// TODO set history
				alert.setErrorMessage();
			}
		}
		return null;
	}

	public Path saveTraceAsTable(Window window) {
		TraceFileHandler traceSaver = injector.getInstance(TraceFileHandler.class);
		Trace possiblyLostTrace = currentTrace.get();
		if (currentTrace.get() != null) {
			try {
				return traceSaver.saveAsTable(possiblyLostTrace);
			} catch (Exception e) {
				final Alert alert = injector.getInstance(StageManager.class).makeAlert(Alert.AlertType.WARNING, "simulation.error.header.invalid", "simulation.error.body.invalid");
				alert.initOwner(window);
				alert.showAndWait();
			}
		}
		return null;
	}

}
