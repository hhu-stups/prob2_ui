package de.prob2.ui.stats;

import java.io.IOException;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.ComputeCoverageCommand;
import de.prob.prolog.term.IntegerPrologTerm;
import de.prob.prolog.term.ListPrologTerm;

import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Singleton
public class StatsView extends AnchorPane {
	@FXML private Label totalTransitions;
	@FXML private GridPane nodeStats;
	@FXML private GridPane transStats;
	
	private final CurrentTrace currentTrace;
	private static final Logger logger = LoggerFactory.getLogger(StatsView.class);
	
	@Inject
	public StatsView(final FXMLLoader loader, final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
		
		loader.setLocation(getClass().getResource("stats_view.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
	}
	
	@FXML
	public void initialize() {
		this.currentTrace.addListener((observable, from, to) -> {
			logger.trace("Trace changed!");
			final ComputeCoverageCommand cmd = new ComputeCoverageCommand();
			if (currentTrace.exists()) {
				logger.trace("Trace exists, asking for coverage data");
				this.currentTrace.getStateSpace().execute(cmd);
				logger.trace("Got coverage data");
				update(cmd.getResult());
				logger.trace("Updated coverage data");
			} else {
				logger.trace("Trace doesn't exist, using dummy coverage data instead");
				update(cmd.new ComputeCoverageResult(
					new IntegerPrologTerm(0),
					new IntegerPrologTerm(0),
					new ListPrologTerm(),
					new ListPrologTerm(),
					new ListPrologTerm()
				));
			}
		});
	}
	
	public void update(ComputeCoverageCommand.ComputeCoverageResult result) {
		Number numTrans = result.getTotalNumberOfTransitions();
		
		Platform.runLater(() -> {
			totalTransitions.setText(String.valueOf(numTrans));
		});
		
		showStats(result.getNodes(), nodeStats);
		showStats(result.getOps(), transStats);
	}
	
	private static void showStats(List<String> packedStats, GridPane grid) {
		final Node[][] stats = new Node[packedStats.size()][];
		
		for (int i = 0; i < packedStats.size(); i++) {
			final String pStat = packedStats.get(i);
			final String woPre = pStat.startsWith("'") ? pStat.substring(1) : pStat;
			final String woSuf = woPre.endsWith("'") ? woPre.substring(0, woPre.length() - 1) : woPre;
			final String[] split = woSuf.split(":");
			final Stat stat;
			if (split.length == 2) {
				stat = new Stat(split[0], split[1]);
			} else if (split.length == 1) {
				stat = new Stat(split[0]);
			} else {
				throw new IllegalArgumentException(String.format("Invalid number of splits (%d, should be 1 or 2) for packed stat: %s", split.length, pStat));
			}
			stats[i] = stat.toFX();
		}
		
		Platform.runLater(() -> {
			grid.getChildren().clear();
			for (int i = 0; i < stats.length; i++) {
				grid.addRow(i+1, stats[i]);
			}
		});
	}
}
