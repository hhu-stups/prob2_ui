package de.prob2.ui.simulation.simulators;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.diagram.DiagramGenerator;
import de.prob2.ui.simulation.diagram.DiagramStage;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.simulation.simulators.check.ISimulationPropertyChecker;
import javafx.application.Platform;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;

@Singleton
public final class RealTimeSimulator extends Simulator {
	private final Scheduler scheduler;

	private final CurrentTrace currentTrace;

	private final UIInteractionHandler uiInteractionHandler;

	private final ChangeListener<Transition> uiListener;

	private DiagramGenerator diagramGenerator;

	@Inject
	public RealTimeSimulator(final CurrentTrace currentTrace, final CurrentProject currentProject, final Provider<ObjectMapper> objectMapperProvider, final Scheduler scheduler, final UIInteractionHandler uiInteractionHandler) {
		super(currentTrace, currentProject, objectMapperProvider);
		this.scheduler = scheduler;
		this.currentTrace = currentTrace;
		this.uiInteractionHandler = uiInteractionHandler;
		this.uiListener = (observable, from, to) -> uiInteractionHandler.handleUserInteraction(this, to);;
		this.diagramGenerator = null;
	}

	public void run() {
		scheduler.run();
		uiInteractionHandler.getLastUserInteraction().addListener(uiListener);
	}

	@FXML
	public void stop() {
		uiInteractionHandler.getLastUserInteraction().removeListener(uiListener);
		scheduler.stop();
	}

	public void simulate() {
		scheduler.startSimulationStep();
		// Read trace and pass it through chooseOperation to avoid race condition
		Trace trace = currentTrace.get();
		try {
			Trace newTrace = simulationStep(trace);
			Trace resultingTrace = newTrace;
			if(currentTrace.get().getCurrentState().isInitialised()) {
				resultingTrace = mergeUserInteractions(trace.getTransitionList().size(), currentTrace.get(), newTrace);
			}
			currentTrace.set(resultingTrace);
		} catch (Exception e) {
			scheduler.endSimulationStep();
			throw e;
		}
		scheduler.endSimulationStep();
		Platform.runLater(()->{
			if (diagramGenerator.getDiaStage()!= null ) {
				if (diagramGenerator.getDiaStage().isShowing() && diagramGenerator.getDiaStage().getIsLive()) {
					diagramGenerator.updateGraph();
				}
			}
		});
	}

	private Trace mergeUserInteractions(int index, Trace traceWithUserInteractions, Trace simulatedTrace) {
		Trace trace = traceWithUserInteractions;
		for(int i = index; i < simulatedTrace.getTransitionList().size(); i++) {
			Transition nextTransition = simulatedTrace.getTransitionList().get(i);
			final Transition op = trace.getCurrentState().getOutTransitions().stream()
					.filter(t -> t.getId().equals(nextTransition.getId()))
					.findAny()
					.orElse(null);
			if(op != null) {
				trace = trace.add(op);
			}
		}
		return trace;
	}

	public BooleanProperty runningProperty() {
		return scheduler.runningProperty();
	}

	public boolean isRunning() {
		return scheduler.isRunning();
	}

	@Override
	public void resetSimulator() {
		super.resetSimulator();
		scheduler.stop();
	}

	@Override
	public boolean endingConditionReached(Trace trace) {
		boolean endingConditionReached = super.endingConditionReached(trace);
		if(config instanceof SimulationModelConfiguration) {
			return endingConditionReached && ((SimulationModelConfiguration) config).getListeners().isEmpty();
		}
		return endingConditionReached;
	}

	@Override
	public void run(ISimulationPropertyChecker simulationPropertyChecker) {
		throw new UnsupportedOperationException();
	}

	public void setDiagramGenerator(DiagramGenerator diagramGenerator) {
		this.diagramGenerator = diagramGenerator;
	}
}
