package de.prob2.ui.states;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.model.representation.AbstractElement;
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
		this.blacklist = FXCollections.observableSet();
		this.knownClasses = FXCollections.observableSet();
	}
	
	public ObservableSet<Class<? extends AbstractElement>> getBlacklist() {
		return this.blacklist;
	}
	
	public ObservableSet<Class<? extends AbstractElement>> getKnownClasses() {
		return this.knownClasses;
	}
}
