package de.prob2.ui;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import de.prob2.ui.modeline.ModelineController;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.Builder;
import javafx.util.BuilderFactory;

public class BFF implements BuilderFactory {

	/**
	 * 
	 */
	private Injector injector;

	@Inject
	public BFF(Injector injector) {
		this.injector = injector;
	}

	BuilderFactory builderFactory = new JavaFXBuilderFactory();

	@Override
	public Builder<?> getBuilder(Class<?> type) {
		System.out.println("Requesting " + type.getSimpleName());
		Binding<?> binding = injector.getExistingBinding(Key.get(type));
		if (binding != null) {
			System.out.println(binding);
			Object instance = injector.getInstance(type);
			System.out.println("Guice " + instance);

			return () -> {
				System.out.println("build " + instance);
				return instance;
			};
		}
		return builderFactory.getBuilder(type);
	}

}