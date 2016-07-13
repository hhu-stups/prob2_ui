package de.prob2.ui.modelchecking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import de.prob.animator.command.ComputeCoverageCommand.ComputeCoverageResult;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLOk;
import de.prob.check.ModelCheckOk;
import de.prob.check.ModelChecker;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.ModelCheckingOptions.Options;
import de.prob.check.StateSpaceStats;
import de.prob.model.representation.AbstractElement;
import de.prob.statespace.ITraceDescription;
import de.prob2.ui.events.ModelCheckStatsEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

public class ModelCheckStats extends AnchorPane implements IModelCheckListener {
	@FXML
	private Text titelText;
	@FXML
	private Label elapsedTime;
	@FXML
	private Label processedNodes;
	@FXML
	private Label totalNodes;
	@FXML
	private Label totalTransitions;
	@FXML
	private GridPane nodeStats;
	@FXML
	private GridPane transStats;

	private Map<String, MCheckJob> jobs = new HashMap<String, MCheckJob>();
	Map<String, IModelCheckingResult> results = new HashMap<String, IModelCheckingResult>();

	private EventBus bus;

	@Inject
	public ModelCheckStats(FXMLLoader loader, EventBus bus) {
		this.bus = bus;
		try {
			loader.setLocation(getClass().getResource("modelchecking_stats.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void updateStats(final String id, final long timeElapsed, final IModelCheckingResult result,
			final StateSpaceStats stats) {
		results.put(id, result);
		Platform.runLater(() -> {
			elapsedTime.setText("" + timeElapsed);
		});
		boolean hasStats = stats != null;

		if (hasStats) {
			int nrProcessedNodes = stats.getNrProcessedNodes();
			int nrTotalNodes = stats.getNrTotalNodes();
			int nrTotalTransitions = stats.getNrTotalTransitions();
			int percent = nrProcessedNodes * 100 / nrTotalNodes;
			Platform.runLater(() -> {
				processedNodes.setText("" + nrProcessedNodes + " (" + percent + " %)");
				totalNodes.setText("" + nrTotalNodes);
				totalTransitions.setText("" + nrTotalTransitions);
			});
		}
		System.out.println("updated Stats");
	}

	@Override
	public void isFinished(final String id, final long timeElapsed, final IModelCheckingResult result,
			final StateSpaceStats stats) {
		results.put(id, result);
		String message = result.getMessage();

		Platform.runLater(() -> {
			elapsedTime.setText("" + timeElapsed);
		});

		String res = result instanceof ModelCheckOk || result instanceof LTLOk ? "success"
				: result instanceof ITraceDescription ? "danger" : "warning";
		boolean hasTrace = result instanceof ITraceDescription;
		ModelChecker modelChecker = jobs.get(id).getChecker();
		ModelCheckingOptions options = jobs.get(id).getOptions();
		ComputeCoverageResult coverage = null;
		Boolean searchForNewErrors = true;

		if (modelChecker != null) {
			coverage = modelChecker.getCoverage();
			AbstractElement main = modelChecker.getStateSpace().getMainComponent();
			List<String> optsList = new ArrayList<String>();
			for (Options opts : options.getPrologOptions()) {
				optsList.add(opts.getDescription());
				if (opts.getDescription().equals("recheck existing states")) {
					searchForNewErrors = false;
				}
			}
			Platform.runLater(() -> {
				String name = main == null ? "Model Check" : main.toString();
				if (!optsList.isEmpty()) {
					name += " with " + Joiner.on(", ").join(optsList);
				}
				titelText.setText(name);
			});
		}

		jobs.remove(id);
		if (coverage != null) {
			Number numNodes = coverage.getTotalNumberOfNodes();
			Number numTrans = coverage.getTotalNumberOfTransitions();

			Platform.runLater(() -> {
				totalNodes.setText("" + numNodes);
				totalTransitions.setText("" + numTrans);
			});

			showStats(coverage.getNodes(), nodeStats);
			showStats(coverage.getOps(), transStats);

			// List<String> uncovered = coverage.getUncovered();
			// for (String transition : uncovered) {
			// transStats.add(WebUtils.wrap("name", transition, "value", "0"));
			// }
			// String transitionStats = WebUtils.toJson(transStats);
		}
		bus.post(new ModelCheckStatsEvent(this, res, message, searchForNewErrors));
		System.out.println("is finished");
		System.out.println(modelChecker.getStateSpace().getMainComponent().toString());
	}

	private void showStats(List<String> packedStats, GridPane grid) {
		Platform.runLater(() -> {
			grid.getChildren().clear();
		});
		for (String pStat : packedStats) {
			String woPre = pStat.startsWith("'") ? pStat.substring(1) : pStat;
			String woSuf = woPre.endsWith("'") ? woPre.substring(0, woPre.length() - 1) : woPre;
			String[] split = woSuf.split(":");
			Stat stat = null;
			if (split.length == 2) {
				stat = new Stat(split[0], split[1]);
			} else if (split.length == 1) {
				stat = new Stat(split[0], null);
			}
			Node[] statFX = stat.toFX();
			Platform.runLater(() -> {
				grid.addRow(packedStats.indexOf(pStat) + 1, statFX);
			});
		}
	}

	void addJob(String jobId, MCheckJob mCheckJob) {
		jobs.put(jobId, mCheckJob);
	}
}
