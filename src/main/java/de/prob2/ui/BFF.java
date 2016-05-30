package de.prob2.ui;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.modeline.ModelineController;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.Builder;
import javafx.util.BuilderFactory;

public class BFF implements BuilderFactory {

	/**
	 * 
	 */
	private Injector injector2;

	@Inject
	public BFF(Injector injector) {
		injector2 = injector;
	}

	BuilderFactory builderFactory = new JavaFXBuilderFactory();

	@Override
	public Builder<?> getBuilder(Class<?> type) {
		System.out.println("Requesting " + type.getSimpleName());
		if (type == ModelineController.class) {
			try {
				Object instance = injector2.getInstance(type);
				System.out.println("Guice " + instance);

				return () -> {
					System.out.println("build " + instance);
					return instance;
				};
			} catch (Exception e) {
			}
		}
		return builderFactory.getBuilder(type);
	}

}