package de.prob2.ui.animation.tracereplay;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class ReplayedTraceRow {

	private final SimpleIntegerProperty step;
	private final SimpleStringProperty fileTransition;
	private final SimpleStringProperty replayedTransition;
	private final SimpleStringProperty precision;
	private final SimpleStringProperty errorMessage;

	public ReplayedTraceRow(int step, String fileTransition, String replayedTransition, String precision, String errorMessage) {
		this.step = new SimpleIntegerProperty(step);
		this.fileTransition = new SimpleStringProperty(fileTransition);
		this.replayedTransition = new SimpleStringProperty(replayedTransition);
		this.precision = new SimpleStringProperty(precision);
		this.errorMessage = new SimpleStringProperty(errorMessage);
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
}
