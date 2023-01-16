package de.prob2.ui.animation.tracereplay;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ReplayedTraceRow {

	private final SimpleIntegerProperty step;
	private final SimpleStringProperty fileTransition;
	private final SimpleStringProperty replayedTransition;
	private final SimpleStringProperty precision;
	private final SimpleStringProperty errorMessage;
	private final SimpleStringProperty style;
	private final List<String> styleClasses;

	public ReplayedTraceRow(int step, String fileTransition, String replayedTransition, String precision, String errorMessage, String style, Collection<String> styleClasses) {
		this.step = new SimpleIntegerProperty(step);
		this.fileTransition = new SimpleStringProperty(fileTransition);
		this.replayedTransition = new SimpleStringProperty(replayedTransition);
		this.precision = new SimpleStringProperty(precision);
		this.errorMessage = new SimpleStringProperty(errorMessage);
		this.style = new SimpleStringProperty(style);
		this.styleClasses = Collections.unmodifiableList(new ArrayList<>(styleClasses));
	}

	public ReadOnlyProperty<Number> stepProperty() {
		return step;
	}

	public ReadOnlyProperty<String> fileTransitionProperty() {
		return fileTransition;
	}

	public ReadOnlyProperty<String> replayedTransitionProperty() {
		return replayedTransition;
	}

	public ReadOnlyProperty<String> precisionProperty() {
		return precision;
	}

	public ReadOnlyProperty<String> errorMessageProperty() {
		return errorMessage;
	}

	public ReadOnlyProperty<String> styleProperty() {
		return style;
	}

	public Collection<String> getStyleClasses() {
		return styleClasses;
	}
}
