package de.prob2.ui.internal;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.Builder;
import javafx.util.BuilderFactory;

public class GuiceBuilderFactory implements BuilderFactory {
	private Injector injector;
	BuilderFactory javafxDefaultBuilderFactory = new JavaFXBuilderFactory();

	@Inject
	public GuiceBuilderFactory(Injector injector) {
		this.injector = injector;
	}

	@Override
	public Builder<?> getBuilder(Class<?> type) {
		if (isGuiceResponsibleForType(type)) {
			Object instance = injector.getInstance(type);
			return () -> instance;
		} else {
			return javafxDefaultBuilderFactory.getBuilder(type);
		}
	}
	
	private boolean isGuiceResponsibleForType(Class<?> type) {
		Binding<?> binding = injector.getExistingBinding(Key.get(type));
		return binding != null;
	}
}
