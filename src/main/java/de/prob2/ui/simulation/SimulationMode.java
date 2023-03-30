package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@Singleton
public class SimulationMode {

	public enum Mode {
		MONTE_CARLO,
		BLACK_BOX
	}

	private final ObjectProperty<Mode> mode;

	@Inject
	public SimulationMode() {
		this.mode = new SimpleObjectProperty<>(null);
	}

	public ObjectProperty<Mode> modeProperty() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode.set(mode);
	}

	public Mode getMode() {
		return mode.get();
	}
}
