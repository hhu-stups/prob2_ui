package de.prob2.ui.tracediff;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.internal.StageManager;
import javafx.scene.Scene;
import javafx.stage.Stage;

@Singleton
public class TraceDiffStage extends Stage {
	private TraceDiff traceDiff;

	@Inject
	public TraceDiffStage(Injector injector) {
		this.traceDiff = injector.getInstance(TraceDiff.class);
		Scene scene = new Scene(traceDiff);
		this.setScene(scene);
		this.initOwner(injector.getInstance(StageManager.class).getMainStage());
	}

	public final void setLists(Trace replayed, PersistentTrace persistent, Trace current) {
		traceDiff.setLists(replayed, persistent, current);
	}

	public void setAlert(TraceReplayErrorAlert alert) {
		traceDiff.setAlert(alert);
	}
}
