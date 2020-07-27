package de.prob2.ui.verifications.modelchecking;

import de.prob.check.StateSpaceStats;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.sharedviews.SimpleStatsView;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class ModelCheckStats extends VBox {
	@FXML private Label elapsedTime;
	@FXML private SimpleStatsView simpleStatsView;
	
	public ModelCheckStats(final StageManager stageManager) {
		stageManager.loadFXML(this, "modelchecking_stats.fxml");
	}

	public void updateStats(final long timeElapsed, final StateSpaceStats stats) {
		Platform.runLater(() -> {
			elapsedTime.setText(String.format("%.1f", timeElapsed / 1000.0) + " s");

			if (stats != null) {
				simpleStatsView.setStats(stats);
			}
		});
	}
}
