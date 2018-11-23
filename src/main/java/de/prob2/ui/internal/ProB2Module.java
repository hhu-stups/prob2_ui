package de.prob2.ui.internal;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.util.Providers;

import de.codecentric.centerdevice.MenuToolkit;
import de.prob.MainModule;
import de.prob2.ui.config.RuntimeOptions;

import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.BoundingBox;
import javafx.util.BuilderFactory;

import org.hildan.fxgson.FxGson;

public class ProB2Module extends AbstractModule {
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
		final MenuToolkit toolkit = IS_MAC && "true".equals(System.getProperty("de.prob2.ui.useMacMenuBar", "true")) ? MenuToolkit.toolkit(locale) : null;
		bind(MenuToolkit.class).toProvider(Providers.of(toolkit));
		bind(RuntimeOptions.class).toInstance(this.runtimeOptions);
		bind(Gson.class).toInstance(FxGson.coreBuilder()
			.disableHtmlEscaping()
			.setPrettyPrinting()
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
			.create());
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
