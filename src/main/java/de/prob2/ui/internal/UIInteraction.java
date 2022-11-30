package de.prob2.ui.internal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Transition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@Singleton
public class UIInteraction {

	private final ObjectProperty<Transition> uiListener;

	@Inject
	public UIInteraction() {
		this.uiListener = new SimpleObjectProperty<>(null);
	}

	public void addUIInteraction(Transition transition) {
		uiListener.set(transition);
	}

	public ObjectProperty<Transition> getUiListener() {
		return uiListener;
	}

	public Transition getLastUIChange() {
		return uiListener.get();
	}
}
