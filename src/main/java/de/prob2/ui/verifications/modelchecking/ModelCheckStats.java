package de.prob2.ui.verifications.modelchecking;

import java.util.Objects;

import de.prob.check.StateSpaceStats;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.sharedviews.SimpleStatsView;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public final class ModelCheckStats extends AnchorPane {
	
	@FXML private VBox statsBox;
	@FXML private Label elapsedTime;
	@FXML private SimpleStatsView simpleStatsView;
	
	public ModelCheckStats(final StageManager stageManager) {
		stageManager.loadFXML(this, "modelchecking_stats.fxml");
	}

	void startJob() {
		statsBox.setVisible(true);
	}

	public void updateStats(final StateSpace stateSpace, final long timeElapsed, final StateSpaceStats stats) {
		Objects.requireNonNull(stateSpace, "stateSpace");

		Platform.runLater(() -> {
			elapsedTime.setText(String.format("%.1f", timeElapsed / 1000.0) + " s");

			if (stats != null) {
				simpleStatsView.setStats(stats);
			}
		});
	}
}
