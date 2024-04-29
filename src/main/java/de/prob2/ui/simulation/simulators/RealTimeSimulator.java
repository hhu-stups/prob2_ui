package de.prob2.ui.simulation.simulators;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.diagram.DiagramGenerator;
import de.prob2.ui.simulation.diagram.DiagramStage;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.simulators.check.ISimulationPropertyChecker;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;

@Singleton
public class RealTimeSimulator extends Simulator {

	private final Scheduler scheduler;

	private final CurrentTrace currentTrace;

	private final UIInteractionHandler uiInteractionHandler;

	private final ChangeListener<Transition> uiListener;

	private DiagramGenerator diagramGenerator;

	private final Injector injector; 

	@Inject
	public RealTimeSimulator(final CurrentTrace currentTrace, final Scheduler scheduler, final UIInteractionHandler uiInteractionHandler, final Injector injector) {
		super(currentTrace);
		this.scheduler = scheduler;
		this.currentTrace = currentTrace;
		this.uiInteractionHandler = uiInteractionHandler;
		this.uiListener = (observable, from, to) -> uiInteractionHandler.handleUserInteraction(this, to);
		this.diagramGenerator = null; 
		this.injector = injector;
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
		Trace newTrace = simulationStep(trace);
		currentTrace.set(newTrace);
		scheduler.endSimulationStep();
		Platform.runLater(()->{
			if (diagramGenerator.getDiaStage()!= null ) {
				if (diagramGenerator.getDiaStage().isShowing() && diagramGenerator.getDiaStage().getIsLive()) {
					diagramGenerator.updateGraph();
				}
			}
		});
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
			return endingConditionReached && ((SimulationModelConfiguration) config).getUiListenerConfigurations().isEmpty();
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
