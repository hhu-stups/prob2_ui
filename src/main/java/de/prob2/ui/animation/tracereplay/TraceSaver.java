package de.prob2.ui.animation.tracereplay;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.stage.Window;

import java.util.Collections;

public class TraceSaver {

    private final Injector injector;

    private final CurrentTrace currentTrace;

    private final CurrentProject currentProject;

    @Inject
    public TraceSaver(final Injector injector, final CurrentTrace currentTrace, final CurrentProject currentProject) {
        this.injector = injector;
        this.currentTrace = currentTrace;
        this.currentProject = currentProject;
    }

    public void saveTrace(Window window, TraceReplayErrorAlert.Trigger trigger) {
        TraceFileHandler traceSaver = injector.getInstance(TraceFileHandler.class);
        Trace copyTrace = currentTrace.get();
        if (currentTrace.get() != null) {
            try {
                traceSaver.save(
                        new PersistentTrace(currentTrace.get(), currentTrace.get().getCurrent().getIndex() + 1),
                        currentProject.getCurrentMachine());
            } catch (Exception e) {
                TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, "traceSave.buttons.saveTrace.error.msg", trigger, Collections.EMPTY_LIST);
                alert.initOwner(window);
                alert.setCopyTrace(copyTrace);
                alert.setErrorMessage();
            }
        }
    }

}
