package de.prob2.ui.unsatcore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.UnsatRegularCoreCommand;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.Join;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.classicalb.Property;
import de.prob.model.representation.AbstractFormulaElement;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@Singleton
public class UnsatCoreCalculator {

	private final CurrentTrace currentTrace;

	private ObjectProperty<IBEvalElement> unsatCore;
	
	private final UnsatCoreStage unsatCoreStage;

	@Inject
	private UnsatCoreCalculator(final CurrentTrace currentTrace, final UnsatCoreStage unsatCoreStage) {
		this.currentTrace = currentTrace;
		this.unsatCore = new SimpleObjectProperty<>(null);
		this.currentTrace.addListener((observable, from, to) -> {
			if(to == null) {
				unsatCore.set(null);
			}
		});
		this.unsatCoreStage = unsatCoreStage;
	}

	public void calculate() {
		ClassicalBModel bModel = (ClassicalBModel) currentTrace.getModel();
		IBEvalElement properties = extractProperties(bModel);
		if(properties == null) {
			return;
		}
		UnsatRegularCoreCommand unsatCoreCmd = new UnsatRegularCoreCommand(properties, new ArrayList<>());
		currentTrace.getStateSpace().execute(unsatCoreCmd);
		IBEvalElement core = unsatCoreCmd.getCore();
		this.unsatCore.set(core);
		this.unsatCoreStage.setUnsatCore(core.getCode());
		this.unsatCoreStage.show();
		
	}

	private static IBEvalElement extractProperties(ClassicalBModel bModel) {
		final List<Property> properties = bModel.getMainMachine().getProperties();
		if (properties.isEmpty()) {
			return null;
		} else {
			final List<IEvalElement> formulas = properties.stream()
				.map(AbstractFormulaElement::getFormula)
				.collect(Collectors.toList());
			return (IBEvalElement) Join.conjunct(bModel, formulas);
		}
	}

	public ObjectProperty<IBEvalElement> unsatCoreProperty() {
		return unsatCore;
	}
}
