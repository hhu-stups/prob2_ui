package de.prob2.ui.sharedviews;

import com.google.inject.Inject;

import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;

public final class NavigationButtonsController {
	@FXML private Button backButton;
	@FXML private Button fastBackButton;
	@FXML private Button forwardButton;
	@FXML private Button fastForwardButton;
	@FXML private Button reloadButton;

	private final CurrentTrace currentTrace;

	private final RealTimeSimulator realTimeSimulator;

	@Inject
	private NavigationButtonsController(final CurrentTrace currentTrace, final RealTimeSimulator realTimeSimulator) {
		super();
		this.currentTrace = currentTrace;
		this.realTimeSimulator = realTimeSimulator;
	}

	@FXML
	private void initialize() {
		BooleanBinding forwardExecutingNextOperationBinding = Bindings.createBooleanBinding(() -> currentTrace.get() != null && !currentTrace.get().canGoForward() && currentTrace.get().getNextTransitions().size() == 1, currentTrace);

		backButton.disableProperty().bind(currentTrace.canGoBackProperty().not().or(realTimeSimulator.runningProperty()));
		fastBackButton.disableProperty().bind(currentTrace.canGoBackProperty().not().or(realTimeSimulator.runningProperty()));
		forwardButton.disableProperty().bind(currentTrace.canGoForwardProperty().not().and(forwardExecutingNextOperationBinding.not()).or(realTimeSimulator.runningProperty()));
		fastForwardButton.disableProperty().bind(currentTrace.canGoForwardProperty().not().or(realTimeSimulator.runningProperty()));

		forwardExecutingNextOperationBinding.addListener((observable, from, to) -> {
			Node graphic = forwardButton.getGraphic();
			graphic.getStyleClass().clear();
			Trace trace = currentTrace.get();
			if(to) {
				State currentState = trace.getCurrentState();
				Transition transition = (Transition) trace.getNextTransitions().toArray()[0];
				if(!currentState.getId().equals(transition.getDestination().getId())) {
					graphic.getStyleClass().add("icon-green");
				} else {
					graphic.getStyleClass().add("icon-dark");
				}
			} else {
				graphic.getStyleClass().add("icon-dark");
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
	private void handleFastForwardButton() {
		final Trace trace = currentTrace.get();
		if (trace != null) {
			currentTrace.set(trace.gotoPosition(trace.size()-1));
		}
	}
}
