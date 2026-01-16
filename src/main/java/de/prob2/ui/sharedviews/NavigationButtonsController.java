package de.prob2.ui.sharedviews;

import com.google.inject.Inject;

import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.internal.executor.FxThreadExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;

public final class NavigationButtonsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(NavigationButtonsController.class);

	@FXML private Button backButton;
	@FXML private Button fastBackButton;
	@FXML private Button forwardButton;
	@FXML private Button fastForwardButton;
	@FXML private MenuButton fastForwardButtonMenu;
	@FXML private MenuItem fiveDeterministicEvents;
	@FXML private MenuItem tenDeterministicEvents;
	@FXML private TextField deterministicText;
	@FXML private Button reloadButton;

	private final CurrentTrace currentTrace;
	private final RealTimeSimulator realTimeSimulator;
	private final CliTaskExecutor cliExecutor;
	private final FxThreadExecutor fxExecutor;
	private final StageManager stageManager;

	@Inject
	private NavigationButtonsController(final CurrentTrace currentTrace, final RealTimeSimulator realTimeSimulator,
	                                    final CliTaskExecutor cliExecutor, final FxThreadExecutor fxExecutor,
	                                    final StageManager stageManager) {
		super();
		this.currentTrace = currentTrace;
		this.realTimeSimulator = realTimeSimulator;
		this.cliExecutor = cliExecutor;
		this.fxExecutor = fxExecutor;
		this.stageManager = stageManager;
	}

	@FXML
	private void initialize() {
		ObjectBinding<Trace> deterministicStepTraceBinding = Bindings.createObjectBinding(() -> {
			Trace t = currentTrace.get();
			if (t != null && !t.canGoForward() && t.getNextTransitions().size() == 1) {
				return t;
			}
			return null;
		}, currentTrace);
		BooleanBinding deterministicStepBinding = Bindings.isNotNull(deterministicStepTraceBinding);

		backButton.disableProperty().bind(currentTrace.canGoBackProperty().not().or(realTimeSimulator.runningProperty()));
		fastBackButton.disableProperty().bind(currentTrace.canGoBackProperty().not().or(realTimeSimulator.runningProperty()));
		forwardButton.disableProperty().bind(currentTrace.canGoForwardProperty().not().and(deterministicStepBinding.not()).or(realTimeSimulator.runningProperty()));
		fastForwardButton.disableProperty().bind(currentTrace.canGoForwardProperty().not().or(deterministicStepBinding).or(realTimeSimulator.runningProperty()));
		fastForwardButtonMenu.disableProperty().bind(currentTrace.canGoForwardProperty().or(deterministicStepBinding.not()).or(realTimeSimulator.runningProperty()));
		fastForwardButton.visibleProperty().bind(fastForwardButtonMenu.disabledProperty());
		fastForwardButton.managedProperty().bind(fastForwardButtonMenu.disabledProperty());
		fastForwardButtonMenu.visibleProperty().bind(fastForwardButton.visibleProperty().not());
		fastForwardButtonMenu.managedProperty().bind(fastForwardButton.managedProperty().not());

		deterministicStepTraceBinding.addListener((observable, from, to) -> {
			Node forwardGraphic = forwardButton.getGraphic();
			Node fastForwardGraphic = fastForwardButtonMenu.getGraphic();
			forwardGraphic.getStyleClass().clear();
			fastForwardGraphic.getStyleClass().clear();
			if (to != null) { // with the above BooleanBinding, we can get an index out of bound exception
				State currentState = to.getCurrentState();
				Transition transition = (Transition) to.getNextTransitions().toArray()[0];
				if (!currentState.getId().equals(transition.getDestination().getId())) {
					forwardGraphic.getStyleClass().add("icon-green");
					fastForwardGraphic.getStyleClass().add("icon-green");
				} else {
					forwardGraphic.getStyleClass().add("icon-dark");
					fastForwardGraphic.getStyleClass().add("icon-dark");
				}
			} else {
				forwardGraphic.getStyleClass().add("icon-dark");
				fastForwardGraphic.getStyleClass().add("icon-dark");
			}
		});
	}

	@FXML
	private void handleBackButton() {
		final Trace trace = currentTrace.get();
		if (trace != null) {
			currentTrace.set(trace.back());
		}
	}

	@FXML
	private void handleFastBackButton() {
		final Trace trace = currentTrace.get();
		if (trace != null) {
			currentTrace.set(trace.gotoPosition(-1));
		}
	}

	@FXML
	private void handleForwardButton() {
		final Trace trace = currentTrace.get();
		if (trace != null) {
			if(trace.canGoForward()) {
				currentTrace.set(trace.forward());
			} else {
				Transition transition = (Transition) trace.getNextTransitions().toArray()[0];
				currentTrace.set(trace.add(transition));
			}
		}
	}

	@FXML
	private void handleFastForwardButton(ActionEvent event) {
		final Trace trace = currentTrace.get();
		if (trace != null) {
			if (trace.canGoForward()) {
				currentTrace.set(trace.gotoPosition(trace.size()-1));
			} else {
				final int operationCount;
				if (event.getSource().equals(deterministicText)) {
					final String deterministicInput = deterministicText.getText();
					if (deterministicInput.isEmpty()) {
						return;
					}
					try {
						operationCount = Integer.parseInt(deterministicInput);
					} catch (NumberFormatException e) {
						LOGGER.error("Invalid input for executing a number of deterministic events",e);
						final Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING,
								"operations.operationsView.alerts.invalidNumberOfOperations.header",
								"operations.operationsView.alerts.invalidNumberOfOperations.content", deterministicInput);
						alert.initOwner(stageManager.getCurrent().getScene().getWindow());
						alert.showAndWait();
						return;
					}
				} else if (event.getSource().equals(fiveDeterministicEvents)) {
					operationCount = 5;
				} else if (event.getSource().equals(tenDeterministicEvents)) {
					operationCount = 10;
				} else {
					throw new AssertionError("Unhandled deterministic animation event source: " + event.getSource());
				}

				Trace currentTrace = this.currentTrace.get();
				if (currentTrace != null) {
					this.cliExecutor.submit(() -> currentTrace.deterministicAnimation(operationCount)).whenCompleteAsync((res, exc) -> {
						if (exc != null) {
							if (!(exc instanceof CancellationException)) {
								LOGGER.error("error while deterministically animating", exc);
								this.stageManager.showUnhandledExceptionAlert(exc, this.stageManager.getCurrent().getScene().getWindow());
							}
						} else if (res != null) {
							this.currentTrace.set(res);
						}
					}, this.fxExecutor);
				}
			}
		}
	}
}
