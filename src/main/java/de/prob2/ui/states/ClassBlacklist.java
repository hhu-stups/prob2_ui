package de.prob2.ui.states;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.Action;

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
		// Hide Action objects by default (they display as source code condensed
		// into a single line otherwise)
		this.blacklist = FXCollections.observableSet(Action.class);
		this.knownClasses = FXCollections.observableSet(Action.class);
	}
	
	public ObservableSet<Class<? extends AbstractElement>> getBlacklist() {
		return this.blacklist;
	}
	
	public ObservableSet<Class<? extends AbstractElement>> getKnownClasses() {
		return this.knownClasses;
	}
}
