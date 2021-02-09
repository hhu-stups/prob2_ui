package de.prob2.ui.simulation.simulators.check;

import com.google.inject.Inject;
import de.prob.check.StateSpaceStats;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@FXMLInjected
public final class SimulationStatsView extends Stage {
	@FXML
	private Label numberSimulations;
	@FXML
	private Label numberSuccess;
	@FXML
	private Label percentage;

	private final ObjectProperty<SimulationStats> stats;

	@Inject
	private SimulationStatsView(final StageManager stageManager) {
		super();
		this.stats = new SimpleObjectProperty<>(this, "stats", null);
		stageManager.loadFXML(this, "simulation_statistics.fxml");
	}
	
	@FXML
	private void initialize() {
		this.statsProperty().addListener((o, from, to) -> {
			if (to == null) {
				numberSimulations.setText(null);
				numberSuccess.setText(null);
				percentage.setText(null);
			} else {
				numberSimulations.setText(String.valueOf(to.getNumberSimulations()));
				numberSuccess.setText(String.valueOf(to.getNumberSuccess()));
				percentage.setText(String.valueOf(to.getPercentage()));
				// TODO: Show extended statistics
			}
		});
	}
	
	public ObjectProperty<SimulationStats> statsProperty() {
		return this.stats;
	}
	
	public SimulationStats getStats() {
		return this.statsProperty().get();
	}
	
	public void setStats(final SimulationStats stats) {
		this.statsProperty().set(stats);
	}
}
