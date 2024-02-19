package de.prob2.ui.railml;

import de.hhu.stups.railml2b.internal.ProgressListener;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.application.Platform;

public class UIProgressListener implements ProgressListener {

	private final ProgressBar progressBar;
	private final Label operation, progressLabel, progressDescription;
	private final int max;

	public UIProgressListener(ProgressBar progressBar, Label operation, Label progressLabel, Label progressDescription, int max) {
		this.progressBar = progressBar;
		this.operation = operation;
		this.progressLabel = progressLabel;
		this.progressDescription = progressDescription;
		this.max = max;
	}

	@Override
	public void updateProgress(int step, String message) {
		Platform.runLater(() -> {
			this.progressBar.setProgress((double) step / max);
			this.operation.setText(message);
			this.progressLabel.setText(" (" + step + "/" + max + ")");
		});
	}

	@Override
	public void updateDescription(String description) {
		Platform.runLater(() -> {
			progressDescription.setText(description);
			progressLabel.setText("");
			progressBar.setProgress(-1);
			operation.setText("");
		});
	}
}
