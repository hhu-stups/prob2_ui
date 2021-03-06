package de.prob2.ui.simulation.simulators.check;

import com.google.inject.Inject;
import de.prob.check.StateSpaceStats;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.stats.Stat;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@FXMLInjected
public final class SimulationStatsView extends Stage {
	@FXML
	private Label numberSimulations;
	@FXML
	private Label numberSuccess;
	@FXML
	private Label percentage;
	@FXML
	private GridPane statisticsPane;

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
				statisticsPane.getChildren().clear();
			} else {
				numberSimulations.setText(String.valueOf(to.getNumberSimulations()));
				numberSuccess.setText(String.valueOf(to.getNumberSuccess()));
				percentage.setText(String.valueOf(to.getPercentage()));

				SimulationExtendedStats extendedStats = to.getExtendedStats();
				List<Stat> stats = new ArrayList<>();
				for(String key : extendedStats.getOperationEnablings().keySet()) {
					int executions = extendedStats.getOperationExecutions().get(key);
					int enablings = extendedStats.getOperationEnablings().get(key);
					BigDecimal percentage = BigDecimal.valueOf(100 * extendedStats.getOperationPercentage().get(key)).setScale(2, RoundingMode.HALF_UP);
					stats.add(new Stat(key, String.format("%s/%s(%s%%)", executions, enablings, percentage)));
				}
				for(int i = 0; i < stats.size(); i++) {
					Stat stat = stats.get(i);
					Node[] nodes = stat.toFX();
					statisticsPane.add(nodes[0], 1, i);
					statisticsPane.add(nodes[1], 2, i);
				}
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
