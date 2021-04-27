package de.prob2.ui.internal;

import java.util.Locale;
import java.util.ResourceBundle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import de.codecentric.centerdevice.MenuToolkit;
import de.prob.MainModule;
import de.prob2.ui.ProB2;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.visualisation.magiclayout.MagicGraphFX;
import de.prob2.ui.visualisation.magiclayout.MagicGraphI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.BuilderFactory;

public class ProB2Module extends AbstractModule {
	public static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

	private final ProB2 application;
	private final RuntimeOptions runtimeOptions;
	private final BuilderFactory javafxDefaultBuilderFactory;

	public ProB2Module(final ProB2 application, final RuntimeOptions runtimeOptions) {
		super();
		this.application = application;
		this.runtimeOptions = runtimeOptions;
		this.javafxDefaultBuilderFactory = new JavaFXBuilderFactory();
	}

	@Override
	protected void configure() {
		install(new MainModule());
		install(new DataPathsModule());

		bind(Locale.class).toProvider(Locale::getDefault);
		bind(Application.class).toInstance(this.application);
		bind(ProB2.class).toInstance(this.application);
		bind(RuntimeOptions.class).toInstance(this.runtimeOptions);

		bind(MagicGraphI.class).to(MagicGraphFX.class);
	}

	@Provides
	@Singleton
	private static ResourceBundle provideResourceBundle(final Locale locale) {
		return ResourceBundle.getBundle("de.prob2.ui.prob2", locale);
	}

	@Provides
	@Singleton
	private static MenuToolkit provideMenuToolkit(final Locale locale) {
		if (IS_MAC && "true".equals(System.getProperty("de.prob2.ui.useMacMenuBar", "true"))) {
			return MenuToolkit.toolkit(locale);
		} else {
			return null;
		}
	}

	@Provides
	public FXMLLoader provideLoader(final Injector injector, ResourceBundle bundle) {
		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setBuilderFactory(type -> {
			if (injector.getExistingBinding(Key.get(type)) != null || type.isAnnotationPresent(FXMLInjected.class)) {
				return () -> injector.getInstance(type);
			} else {
				return javafxDefaultBuilderFactory.getBuilder(type);
			}
		});
		fxmlLoader.setControllerFactory(injector::getInstance);
		fxmlLoader.setResources(bundle);
		return fxmlLoader;
	}

	@Provides
	private static ObjectMapper provideObjectMapper() {
		return new ObjectMapper().registerModule(new ProB2UIJacksonModule());
	}
}
