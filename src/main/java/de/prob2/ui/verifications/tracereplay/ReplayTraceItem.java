package de.prob2.ui.verifications.tracereplay;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.verifications.tracereplay.ReplayTrace.Status;
import javafx.scene.paint.Color;

public class ReplayTraceItem {

	private FontAwesomeIconView statusIcon;
	private final String name;

	public ReplayTraceItem(ReplayTrace trace, String name) {
		this.name = name;
		this.setStatus(trace.getStatus().get());
		trace.getStatus().addListener((observable, from, to) -> setStatus(to));
	}

	private void setStatus(Status status) {
		switch (status) {
		case SUCCESSFUL:
			this.statusIcon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
			this.statusIcon.setFill(Color.GREEN);
			break;
		case FAILED:
			this.statusIcon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);
			this.statusIcon.setFill(Color.RED);
			break;
		default:
			this.statusIcon = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
			this.statusIcon.setFill(Color.BLUE);
			break;
		}
	}
	
	public FontAwesomeIconView getStatusIcon() {
		return statusIcon;
	}

	public String getName() {
		return name;
	}
}
