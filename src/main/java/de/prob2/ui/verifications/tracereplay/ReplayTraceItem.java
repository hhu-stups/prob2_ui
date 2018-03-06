package de.prob2.ui.verifications.tracereplay;

import java.io.File;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.verifications.tracereplay.ReplayTrace.Status;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class ReplayTraceItem {

	private ObjectProperty<FontAwesomeIconView> statusIcon;
	private final String name;
	private final File location;
	private final ReplayTrace trace;

	public ReplayTraceItem(ReplayTrace trace, File traceFile) {
		this.name = traceFile.getAbsolutePath();
		this.location = traceFile;
		this.trace = trace;
		this.statusIcon = new SimpleObjectProperty<>();
		this.setStatus(trace.getStatus().get());
		trace.getStatus().addListener((observable, from, to) -> setStatus(to));
	}

	private void setStatus(Status status) {
		FontAwesomeIconView icon;
		switch (status) {
		case SUCCESSFUL:
			icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
			icon.setFill(Color.GREEN);
			break;
		case FAILED:
			icon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);
			icon.setFill(Color.RED);
			break;
		default:
			icon = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
			icon.setFill(Color.BLUE);
			break;
		}
		this.statusIcon.set(icon);
	}

	public FontAwesomeIconView getStatusIcon() {
		return statusIcon.get();
	}
	
	public ObjectProperty<FontAwesomeIconView> statusIconProperty() {
		return statusIcon;
	}

	public String getName() {
		return name;
	}
	
	public File getLocation() {
		return location;
	}
	
	public ReplayTrace getTrace() {
		return trace;
	}
}
