package de.prob2.ui.modelchecking;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.animator.command.ComputeCoverageCommand.ComputeCoverageResult;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLOk;
import de.prob.check.ModelCheckOk;
import de.prob.check.ModelChecker;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class ModelcheckingView extends AnchorPane implements IModelCheckListener {

	@FXML
	private CheckBox findDeadlocks;
	@FXML
	private CheckBox findInvViolations;
	@FXML
	private CheckBox findBAViolations;
	@FXML
	private CheckBox findGoal;
	@FXML
	private CheckBox stopAtFullCoverage;
	@FXML
	private CheckBox searchForNewErrors;

	private AnimationSelector animations;
	private Map<String, ModelChecker> jobs = new HashMap<String, ModelChecker>();
	
	@Inject
	public ModelcheckingView(AnimationSelector ANIMATIONS, FXMLLoader loader) {
		this.animations = ANIMATIONS;
		try {
			loader.setLocation(getClass().getResource("modelchecking_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	void startModelCheck(ActionEvent event) {
		if (animations.getCurrentTrace() == null) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Specification file missing");
			alert.setHeaderText("No specification file loaded. Cannot run model checker.");
			alert.showAndWait();
			return;
		}
		ModelCheckingOptions options = getOptions();
		StateSpace currentStateSpace = animations.getCurrentTrace().getStateSpace();
		ModelChecker checker = new ModelChecker(new ConsistencyChecker(currentStateSpace, options, null, this));
		jobs.put(checker.getJobId(), checker);
		checker.start();

		// AbstractElement main = currentStateSpace.getMainComponent();
		// String name = main == null ? "Model Check" : main.toString();
		// List<String> ss = new ArrayList<String>();
		// for (Options opts : options.getPrologOptions()) {
		// ss.add(opts.getDescription());
		// }
		// if (!ss.isEmpty()) {
		// name += " with " + Joiner.on(", ").join(ss);
		// }
	}

	private ModelCheckingOptions getOptions() {
		ModelCheckingOptions options = new ModelCheckingOptions();
		options = options.breadthFirst(true);
		options = options.checkDeadlocks(findDeadlocks.isSelected());
		options = options.checkInvariantViolations(findInvViolations.isSelected());
		options = options.checkAssertions(findBAViolations.isSelected());
		options = options.checkGoal(findGoal.isSelected());
		options = options.stopAtFullCoverage(stopAtFullCoverage.isSelected());

		return options;
	}

	@FXML
	void cancel(ActionEvent event) {
		Stage stage = (Stage) this.getScene().getWindow();
		stage.close();
	}

	@Override
	public void updateStats(final String id, final long timeElapsed,
			final IModelCheckingResult result, final StateSpaceStats stats) {
		// results.put(id, result);
		boolean hasStats = stats != null;

		if (hasStats) {
			int nrProcessedNodes = stats.getNrProcessedNodes();
			int nrTotalNodes = stats.getNrTotalNodes();
			int percent = nrProcessedNodes * 100 / nrTotalNodes;
			System.out.println("elapsed Time: " + timeElapsed + "processed Nodes: " + nrProcessedNodes
					+ " total Nodes: " + nrTotalNodes + " percent: " + percent);
			// submit(WebUtils.wrap("cmd", "ModelChecking.updateJob", "id", id,
			// "stats", hasStats, "processedNodes", nrProcessedNodes,
			// "totalNodes", nrTotalNodes, "totalTransitions",
			// stats.getNrTotalTransitions(), "percent", percent, "time",
			// timeElapsed));
		}
		// else {
		// submit(WebUtils.wrap("cmd", "ModelChecking.updateJob", "id", id,
		// "stats", hasStats, "percent", 100, "time", timeElapsed));
		// }
		System.out.println("updated Stats");
	}

	@Override
	public void isFinished(final String id, final long timeElapsed,
			final IModelCheckingResult result, final StateSpaceStats stats) {
//		results.put(id, result);

		String res = result instanceof ModelCheckOk || result instanceof LTLOk ? "success"
				: result instanceof ITraceDescription ? "danger" : "warning";
		boolean hasTrace = result instanceof ITraceDescription;
		ModelChecker modelChecker = jobs.get(id);
		ComputeCoverageResult coverage = null;
		
		if (modelChecker != null) {
			coverage = modelChecker.getCoverage();
		}

		jobs.remove(id);
		if (coverage != null) {
			Number numNodes = coverage.getTotalNumberOfNodes();
			Number numTrans = coverage.getTotalNumberOfTransitions();
//
//			String nodeStats = WebUtils.toJson(extractNodeStats(coverage
//					.getNodes()));
//			List<Map<String, String>> transStats = extractNodeStats(coverage
//					.getOps());
//			List<String> uncovered = coverage.getUncovered();
//			for (String transition : uncovered) {
//				transStats.add(WebUtils.wrap("name", transition, "value", "0"));
//			}
//			String transitionStats = WebUtils.toJson(transStats);
//			submit(WebUtils.wrap("cmd", "ModelChecking.finishJob", "id", id,
//					"time", timeElapsed, "stats", true, "processedNodes",
//					numNodes, "totalNodes", numNodes, "totalTransitions",
//					numTrans, "result", res, "hasTrace", hasTrace, "message",
//					result.getMessage(), "nodeStats", nodeStats, "transStats",
//					transitionStats));
		} 
//		else {
//			Map<String, String> wrap = WebUtils.wrap("cmd",
//					"ModelChecking.finishJob", "id", id, "time", timeElapsed,
//					"stats", false, "result", res, "hasTrace", hasTrace,
//					"message", result.getMessage());
//			submit(wrap);
//		}

		System.out.println("is finished");
	}
}
