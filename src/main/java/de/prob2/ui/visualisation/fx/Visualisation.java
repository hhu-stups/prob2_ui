package de.prob2.ui.visualisation.fx;

import de.prob2.ui.visualisation.fx.listener.EventListener;
import de.prob2.ui.visualisation.fx.listener.FormulaListener;

import javafx.scene.Node;

public abstract class Visualisation {

	private VisualisationController controller;
	protected VisualisationModel model;

	protected abstract String getName();

	protected abstract String[] getMachines();

	protected abstract Node initialize();

	protected abstract void stop();

	protected abstract void registerFormulaListener();

	protected abstract void registerEventListener();

	public final void setController(VisualisationController controller) {
		this.controller = controller;
	}

	public final void setModel(VisualisationModel model) {
		this.model = model;
	}

	protected final void registerFormulaListener(FormulaListener listener) {
		controller.registerFormulaListener(listener);
	}

	protected final void registerEventListener(EventListener listener) {
		controller.registerEventListener(listener);
	}
}
