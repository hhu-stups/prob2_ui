package de.prob2.ui.sharedviews;

import com.google.inject.Inject;

import de.prob.check.StateSpaceStats;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

@FXMLInjected
public final class SimpleStatsView extends VBox {
	@FXML
	private Label processedStates;
	@FXML
	private Label totalTransitions;
	
	private final ObjectProperty<StateSpaceStats> stats;
	
	@Inject
	private SimpleStatsView(final StageManager stageManager) {
		super();
		
		this.stats = new SimpleObjectProperty<>(this, "stats", null);
		
		stageManager.loadFXML(this, "simple_stats_view.fxml");
	}
	
	@FXML
	private void initialize() {
		this.statsProperty().addListener((o, from, to) -> {
			if (to == null) {
				processedStates.setText(null);
				totalTransitions.setText(null);
			} else {
				String processedStatesDescription = String.format("%d/%d", to.getNrProcessedNodes(), to.getNrTotalNodes());
				if (to.getNrTotalNodes() != 0) {
					final int percentProcessedNodes = 100 * to.getNrProcessedNodes() / to.getNrTotalNodes();
					processedStatesDescription += " (" + percentProcessedNodes + "%)";
				}
				processedStates.setText(processedStatesDescription);
				totalTransitions.setText(Integer.toString(to.getNrTotalTransitions()));
			}
		});
	}
	
	public ObjectProperty<StateSpaceStats> statsProperty() {
		return this.stats;
	}
	
	public StateSpaceStats getStats() {
		return this.statsProperty().get();
	}
	
	public void setStats(final StateSpaceStats stats) {
		this.statsProperty().set(stats);
	}
}
