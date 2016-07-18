package de.prob2.ui.states;

import java.util.HashSet;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.javafx.collections.ObservableSetWrapper;
import de.prob.model.representation.AbstractElement;
import javafx.collections.ObservableSet;

@Singleton
public class ClassBlacklist {
	private final ObservableSet<Class<? extends AbstractElement>> blacklist;
	private final ObservableSet<Class<? extends AbstractElement>> knownClasses;
	
	@Inject
	public ClassBlacklist() {
		super();
		this.blacklist = new ObservableSetWrapper<>(new HashSet<>());
		this.knownClasses = new ObservableSetWrapper<>(new HashSet<>());
	}
	
	public ObservableSet<Class<? extends AbstractElement>> getBlacklist() {
		return this.blacklist;
	}
	
	public ObservableSet<Class<? extends AbstractElement>> getKnownClasses() {
		return this.knownClasses;
	}
}
