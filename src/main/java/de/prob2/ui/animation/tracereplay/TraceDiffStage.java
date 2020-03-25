package de.prob2.ui.animation.tracereplay;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

@Singleton
public class TraceDiffStage extends Stage {
	private TraceDiff traceDiff;

	@Inject
	public TraceDiffStage(Injector injector) {
		this.traceDiff = injector.getInstance(TraceDiff.class);
		Scene scene = new Scene(traceDiff);
		this.setScene(scene);
	}

	void setLists(Trace replayed, PersistentTrace persistent, Trace current) {
		traceDiff.setLists(replayed, persistent, current);
	}
}
