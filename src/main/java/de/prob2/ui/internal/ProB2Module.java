package de.prob2.ui.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.*;
import de.jangassen.MenuToolkit;
import de.prob.MainModule;
import de.prob2.ui.ProB2;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.menu.OpenFile;
import de.prob2.ui.menu.RevealInExplorer;
import de.prob2.ui.visualisation.magiclayout.MagicGraphFX;
import de.prob2.ui.visualisation.magiclayout.MagicGraphI;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.Builder;
import javafx.util.BuilderFactory;

import java.util.Locale;
import java.util.ResourceBundle;

public class ProB2Module extends AbstractModule {

	public static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
	public static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

	private static final BuilderFactory javafxDefaultBuilderFactory = new JavaFXBuilderFactory();

	private final ProB2 application;
	private final RuntimeOptions runtimeOptions;

	public ProB2Module(final ProB2 application, final RuntimeOptions runtimeOptions) {
		super();
		this.application = application;
		this.runtimeOptions = runtimeOptions;
	}

	@Provides
	private static Locale locale(/*UIState uiState*/) {
		return Locale.getDefault();
		// this code allows changing the locale at runtime
		/*Locale localeOverride = uiState.getLocaleOverride();
		return localeOverride != null ? localeOverride : Locale.getDefault();*/
	}

	@Provides
	private static ResourceBundle resourceBundle(I18n i18n) {
		return i18n.bundle();
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
	@Singleton
	private static RevealInExplorer provideRevealInExplorer(StageManager stageManager) {
		if (IS_WINDOWS) {
			return new RevealInExplorer.ExplorerSelect(stageManager);
		} else {
			return new RevealInExplorer.DesktopOpen(stageManager);
		}
	}

	@Provides
	@Singleton
	private static OpenFile provideOpenFile(StageManager stageManager) {
		return new OpenFile.DesktopOpen(stageManager);
	}

	@Provides
	public static ObjectMapper provideObjectMapper() {
		return new ObjectMapper()
			       .registerModule(new ParameterNamesModule())
			       .registerModule(new Jdk8Module())
			       .registerModule(new JavaTimeModule())
			       .registerModule(new GuavaModule())
			       .registerModule(new ProB2UIJacksonModule());
	}


	@Provides
	private static FXMLLoader provideLoader(final Injector injector, I18n i18n) {
		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setBuilderFactory(type -> {
			if (injector.getExistingBinding(Key.get(type)) != null || type.isAnnotationPresent(FXMLInjected.class)) {
				// this allows FXML to configure instances, remember to implement Builder and return "this" in the build method.
				if (Builder.class.isAssignableFrom(type)) {
					return (Builder<?>) injector.getInstance(type);
				}

				return () -> injector.getInstance(type);
			} else {
				return javafxDefaultBuilderFactory.getBuilder(type);
			}
		});
		fxmlLoader.setControllerFactory(injector::getInstance);
		fxmlLoader.setResources(i18n.bundle());
		return fxmlLoader;
	}

	@Override
	protected void configure() {
		install(new MainModule());
		install(new DataPathsModule());

		bind(Application.class).toInstance(this.application);
		bind(ProB2.class).toInstance(this.application);
		bind(RuntimeOptions.class).toInstance(this.runtimeOptions);

		bind(MagicGraphI.class).to(MagicGraphFX.class);
	}
}
