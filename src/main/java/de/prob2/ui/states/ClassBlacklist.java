package de.prob2.ui.states;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.classicalb.Assertion;
import de.prob.model.classicalb.Constraint;
import de.prob.model.classicalb.Parameter;
import de.prob.model.classicalb.Property;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.Action;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Constant;
import de.prob.model.representation.Guard;
import de.prob.model.representation.Invariant;
import de.prob.model.representation.Set;
import de.prob.model.representation.Variable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

@Singleton
public final class ClassBlacklist {
	private final ObservableSet<Class<? extends AbstractElement>> blacklist;
	private final ObservableSet<Class<? extends AbstractElement>> knownClasses;
	
	@Inject
	@SuppressWarnings("unchecked")
	private ClassBlacklist() {
		super();
		// Some classes are hidden in the default config file
		this.blacklist = FXCollections.observableSet();
		this.knownClasses = FXCollections.observableSet(
			Action.class,
			Assertion.class,
			BEvent.class,
			Constant.class,
			Constraint.class,
			Guard.class,
			Invariant.class,
			Parameter.class,
			Property.class,
			Set.class,
			Variable.class
		);
	}
	
	public ObservableSet<Class<? extends AbstractElement>> getBlacklist() {
		return this.blacklist;
	}
	
	public ObservableSet<Class<? extends AbstractElement>> getKnownClasses() {
		return this.knownClasses;
	}
}
