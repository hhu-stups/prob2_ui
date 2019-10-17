package de.prob2.ui.internal;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.util.Providers;

import de.codecentric.centerdevice.MenuToolkit;
import de.prob.MainModule;
import de.prob.scripting.StateSpaceProvider;
import de.prob.statespace.StateSpace;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.error.WarningAlert;
import de.prob2.ui.visualisation.magiclayout.MagicGraphFX;
import de.prob2.ui.visualisation.magiclayout.MagicGraphI;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.BoundingBox;
import javafx.scene.paint.Color;
import javafx.util.BuilderFactory;

import org.hildan.fxgson.FxGson;
import org.hildan.fxgson.adapters.extras.ColorTypeAdapter;

public class ProB2Module extends AbstractModule {
	/**
	 * Custom {@link StateSpaceProvider} subclass that adds a warning listener to every created {@link StateSpace}, so that ProB warnings are displayed in the UI.
	 */
	private static final class CustomStateSpaceProvider extends StateSpaceProvider {
		@Inject
		private CustomStateSpaceProvider(
			final Provider<StateSpace> ssProvider,
			final Provider<StageManager> stageManagerProvider
		) {
			super(() -> {
				final StateSpace stateSpace = ssProvider.get();
				stateSpace.addWarningListener(warnings -> Platform.runLater(() ->
					new WarningAlert(stageManagerProvider.get(), warnings).show()
				));
				return stateSpace;
			});
		}
	}

	public static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

	private final RuntimeOptions runtimeOptions;
	private final BuilderFactory javafxDefaultBuilderFactory;

	public ProB2Module(final RuntimeOptions runtimeOptions) {
		super();
		this.runtimeOptions = runtimeOptions;
		this.javafxDefaultBuilderFactory = new JavaFXBuilderFactory();
	}

	@Override
	protected void configure() {
		install(new MainModule());

		// General stuff
		final Locale locale = Locale.getDefault();
		bind(Locale.class).toInstance(locale);
		final ResourceBundle bundle = ResourceBundle.getBundle("de.prob2.ui.prob2", locale);
		bind(ResourceBundle.class).toInstance(bundle);
		final MenuToolkit toolkit = IS_MAC && "true".equals(System.getProperty("de.prob2.ui.useMacMenuBar", "true"))
				? MenuToolkit.toolkit(locale)
				: null;
		bind(MenuToolkit.class).toProvider(Providers.of(toolkit));
		bind(RuntimeOptions.class).toInstance(this.runtimeOptions);
		bind(Gson.class).toInstance(FxGson.coreBuilder()
			.disableHtmlEscaping()
			.setPrettyPrinting()
			.addSerializationExclusionStrategy(new ExclusionStrategy() {
				@Override
				public boolean shouldSkipField(final FieldAttributes f) {
					return f.getAnnotation(OnlyDeserialize.class) != null;
				}
				
				@Override
				public boolean shouldSkipClass(final Class<?> clazz) {
					return false;
				}
			})
			.registerTypeAdapter(Class.class, (JsonSerializer<Class<?>>)(src, typeOfSrc, context) -> context.serialize(src.getName()))
			.registerTypeAdapter(Class.class, (JsonDeserializer<Class<?>>)(json, typeOfT, context) -> {
				try {
					return Class.forName(json.getAsString());
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException(e);
				}
			})
			.registerTypeAdapter(File.class, (JsonSerializer<File>)(src, typeOfSrc, context) -> context.serialize(src.getPath()))
			.registerTypeAdapter(File.class, (JsonDeserializer<File>)(json, typeOfT, context) -> new File(json.getAsString()))
			.registerTypeAdapter(Path.class, (JsonSerializer<Path>)(src, typeOfSrc, context) -> context.serialize(src.toString()))
			.registerTypeAdapter(Path.class, (JsonDeserializer<Path>)(json, typeOfT, context) -> Paths.get(json.getAsString()))
			.registerTypeAdapter(BoundingBox.class, (JsonSerializer<BoundingBox>)(src, typeOfSrc, context) ->
				context.serialize(new double[] {src.getMinX(), src.getMinY(), src.getWidth(), src.getHeight()})
			)
			.registerTypeAdapter(BoundingBox.class, (JsonDeserializer<BoundingBox>)(json, typeOfT, context) -> {
				final double[] array = context.deserialize(json, double[].class);
				if (array.length != 4) {
					throw new IllegalArgumentException("JSON array representing a BoundingBox must have exactly 4 elements");
				}
				return new BoundingBox(array[0], array[1], array[2], array[3]);
			})
			.registerTypeAdapter(Color.class, new ColorTypeAdapter())
			.create());
		
		bind(StateSpaceProvider.class).to(CustomStateSpaceProvider.class);
		
		bind(MagicGraphI.class).to(MagicGraphFX.class);
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
}
