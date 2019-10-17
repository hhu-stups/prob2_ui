package de.prob2.ui.internal;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.codecentric.centerdevice.MenuToolkit;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.error.ExceptionAlert;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.persistence.UIPersistence;
import de.prob2.ui.persistence.UIState;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.BoundingBox;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * This singleton provides common methods for creating and initializing views,
 * dialogs and stages. These methods ensure that all parts of the ProB 2 UI
 * use the correct visual styles and are known to internal mechanisms like
 * the UI persistence and Mac menu bar handling.
 * 
 * @see FileChooserManager
 * @see UIPersistence
 */
@Singleton
public final class StageManager {
	private enum PropertiesKey {
		PERSISTENCE_ID, USE_GLOBAL_MAC_MENU_BAR
	}

	private static final String STYLESHEET = "prob.css";
	private static final Image ICON = new Image(StageManager.class.getResource("/de/prob2/ui/ProB_Icon.png").toExternalForm());

	private final Injector injector;
	private final MenuToolkit menuToolkit;
	private final UIState uiState;
	private final ResourceBundle bundle;

	private final ObjectProperty<Stage> current;
	private final Map<Stage, Void> registered;
	private MenuBar globalMacMenuBar;
	private Stage mainStage;
	private final DoubleProperty stageSceneWidthDifference;
	private final DoubleProperty stageSceneHeightDifference;

	@Inject
	private StageManager(final Injector injector, @Nullable final MenuToolkit menuToolkit, final UIState uiState, final ResourceBundle bundle) {
		this.injector = injector;
		this.menuToolkit = menuToolkit;
		this.uiState = uiState;
		this.bundle = bundle;

		this.current = new SimpleObjectProperty<>(this, "current");
		this.registered = new WeakHashMap<>();
		this.globalMacMenuBar = null;
		this.stageSceneWidthDifference = new SimpleDoubleProperty(this, "stageSceneWidthDifference", 0.0);
		this.stageSceneHeightDifference = new SimpleDoubleProperty(this, "stageSceneHeightDifference", 0.0);
	}

	/**
	 * Load an FXML file with {@code controller} as the root and controller.
	 *
	 * @param controller the object to use as the FXML file's root
	 * and controller
	 * @param fxmlUrl the URL of the FXML file to load
	 */
	public void loadFXML(final Object controller, final URL fxmlUrl) {
		final FXMLLoader loader = injector.getInstance(FXMLLoader.class);
		loader.setLocation(fxmlUrl);
		loader.setRoot(controller);
		loader.setController(controller);
		try {
			loader.load();
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}

		final FontSize fontSize = injector.getInstance(FontSize.class);
		final StringExpression fontSizeCssValue = Bindings.format("-fx-font-size: %dpx;", fontSize.fontSizeProperty());
		if (controller instanceof Node) {
			Node controllerNode = (Node) controller;
			controllerNode.styleProperty().bind(fontSizeCssValue);
		} else if (controller instanceof Stage) {
			Stage controllerStage = (Stage) controller;
			controllerStage.getScene().getRoot().styleProperty().bind(fontSizeCssValue);
		} else if (controller instanceof Dialog<?>) {
			Dialog<?> controllerDialog = (Dialog<?>) controller;
			controllerDialog.getDialogPane().styleProperty().bind(fontSizeCssValue);
		}
	}

	/**
	 * Load an FXML file with {@code controller} as the root and controller.
	 * {@code fxmlResource} is the resource name of the FXML file to load.
	 * If {@code fxmlResource} is a relative name (i. e. one that doesn't start
	 * with a slash), it is resolved relative to {@code controller}'s class.
	 *
	 * @param controller the object to use as the FXML file's root
	 * and controller
	 * @param fxmlResource the resource name of the FXML file to load
	 */
	public void loadFXML(final Object controller, final String fxmlResource) {
		this.loadFXML(controller, controller.getClass().getResource(fxmlResource));
	}

	/**
	 * <p>Load an FXML file with the {@link Stage} {@code controller} as the
	 * root and controller, initialize it, and register it with the
	 * UI persistence mechanism.</p>
	 * 
	 * <p>This is equivalent to loading the FXML file using
	 * {@link #loadFXML(Object, String)} and then registering the stage using
	 * {@link #register(Stage, String)}.</p>
	 *
	 * @param controller the {@link Stage} to use as the FXML file's root
	 * and controller
	 * @param fxmlResource the resource name of the FXML file to load
	 * @param persistenceID a string identifying the stage for UI persistence,
	 * or {@code null} if the stage should not be persisted
	 */
	public void loadFXML(final Stage controller, final String fxmlResource, final String persistenceID) {
		this.loadFXML((Object) controller, fxmlResource);
		this.register(controller, persistenceID);
	}

	/**
	 * <p>Load an FXML file with the {@link Stage} {@code controller} as the
	 * root and controller, and initialize it without registering it with the
	 * UI persistence mechanism.</p>
	 * 
	 * <p>This is equivalent to loading the FXML file using
	 * {@link #loadFXML(Stage, String, String)} with {@code null} as the
	 * persistence ID (which disables persistence for the stage).</p>
	 *
	 * @param controller the {@link Stage} to use as the FXML file's root
	 * and controller
	 * @param fxmlResource the resource name of the FXML file to load
	 */
	public void loadFXML(final Stage controller, final String fxmlResource) {
		this.loadFXML(controller, fxmlResource, null);
	}

	/**
	 * Initialize the given stage and register it with the UI persistence
	 * mechanism. The stage must already have its scene
	 * {@link Stage#setScene(Scene) set}.
	 *
	 * @param stage the stage to register
	 * @param persistenceID a string identifying the stage for UI persistence,
	 * or {@code null} if the stage should not be persisted
	 */
	public void register(final Stage stage, final String persistenceID) {
		this.registered.put(stage, null);
		setPersistenceID(stage, persistenceID);
		stage.getProperties().putIfAbsent(PropertiesKey.USE_GLOBAL_MAC_MENU_BAR, true);
		stage.getScene().getStylesheets().add(STYLESHEET);
		stage.getIcons().add(ICON);

		// If possible, make the stage respect the minimum size of its content.
		// For some reason, this is not the default behavior in JavaFX.
		// Loosely based on https://community.oracle.com/thread/2511660
		final ChangeListener<Parent> rootListener = (o, from, to) -> {
			if (to instanceof Region) {
				final Region region = (Region)to;
				stage.minWidthProperty().bind(
					Bindings.createDoubleBinding(
						() -> region.minWidth(Region.USE_COMPUTED_SIZE),
						region.minWidthProperty(),
						region.widthProperty()
					).add(this.stageSceneWidthDifference)
				);
				stage.minHeightProperty().bind(
					Bindings.createDoubleBinding(
						() -> region.minHeight(Region.USE_COMPUTED_SIZE),
						region.minHeightProperty(),
						region.heightProperty()
					).add(this.stageSceneHeightDifference)
				);
			} else {
				stage.minWidthProperty().unbind();
				stage.minHeightProperty().unbind();
			}
		};
		final ChangeListener<Scene> sceneListener = (o, from, to) -> {
			if (from != null) {
				from.rootProperty().removeListener(rootListener);
			}
			if (to != null) {
				to.rootProperty().addListener(rootListener);
				rootListener.changed(to.rootProperty(), null, to.getRoot());
			}
		};
		stage.sceneProperty().addListener(sceneListener);
		sceneListener.changed(stage.sceneProperty(), null, stage.getScene());

		stage.focusedProperty().addListener(e -> {
			final String stageId = getPersistenceID(stage);
			if (stageId != null) {
				injector.getInstance(UIState.class).moveStageToEnd(stageId);
			}
		});

		stage.showingProperty().addListener((observable, from, to) -> {
			final String stageId = getPersistenceID(stage);
			if (stageId != null) {
				if (to) {
					final BoundingBox box = uiState.getSavedStageBoxes().get(stageId);
					if (box != null) {
						stage.setX(box.getMinX());
						stage.setY(box.getMinY());
						stage.setWidth(box.getWidth());
						stage.setHeight(box.getHeight());
					}
					uiState.getStages().put(stageId, new WeakReference<>(stage));
					uiState.getSavedVisibleStages().add(stageId);
				} else {
					uiState.getSavedVisibleStages().remove(stageId);
					uiState.getSavedStageBoxes().put(stageId,
							new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()));
				}
			}
		});
		// Workaround for JavaFX bug JDK-8224260 (https://bugs.openjdk.java.net/browse/JDK-8224260):
		// Add a second ChangeListener that does nothing.
		stage.showingProperty().addListener((o, from, to) -> {});

		stage.focusedProperty().addListener((observable, from, to) -> {
			if (to) {
				this.current.set(stage);
			} else if (stage.equals(this.current.get())) {
				this.current.set(null);
			}
		});

		if (this.menuToolkit != null && this.globalMacMenuBar != null && isUseGlobalMacMenuBar(stage)) {
			this.menuToolkit.setMenuBar(stage, this.globalMacMenuBar);
		}
	}

	/**
	 * Initialize the given stage as the ProB 2 UI's main stage and register
	 * it with the UI persistence mechanism. The stage must already have its
	 * scene {@link Stage#setScene(Scene) set}.
	 *
	 * @param stage the stage to register as the main stage
	 * @param persistenceID a string identifying the stage for UI persistence,
	 * or {@code null} if the stage should not be persisted
	 */
	public void registerMainStage(Stage stage, String persistenceID) {
		this.mainStage = stage;
		stage.setOnShown(event -> {
			this.stageSceneWidthDifference.set(stage.getWidth() - stage.getScene().getWidth());
			this.stageSceneHeightDifference.set(stage.getHeight() - stage.getScene().getHeight());
			stage.setOnShown(null);
		});
		this.register(stage, persistenceID);
	}

	/**
	 * Create a new stage with the given {@link Scene} as its scene,
	 * initialize it, and register it with the UI persistence mechanism.
	 *
	 * @param scene the new stage's scene
	 * @param persistenceID a string identifying the stage for UI persistence,
	 * or {@code null} if the stage should not be persisted
	 * @return a new stage with the given scene
	 */
	public Stage makeStage(final Scene scene, final String persistenceID) {
		final Stage stage = new Stage();
		stage.setScene(scene);
		this.register(stage, persistenceID);
		return stage;
	}

	/**
	 * Initialize the given dialog.
	 *
	 * @param dialog the dialog to register
	 */
	public void register(final Dialog<?> dialog) {
		dialog.getDialogPane().getStylesheets().add(STYLESHEET);
	}

	/**
	 * Create and initialize a new alert with custom buttons.
	 *
	 * @param alertType the alert type
	 * @param buttons the custom buttons
	 * @param headerBundleKey the resource bundle key for the alert header
	 * text, or {@code ""} for the default header text provided by JavaFX
	 * @param contentBundleKey the resource bundle key for the alert content
	 * text, whose localized value may contain {@link Formatter}-style
	 * placeholders
	 * @param contentParams the objects to insert into the placeholders in
	 * {@code contentBundleKey}'s localized value
	 * @return a new alert
	 */
	public Alert makeAlert(final Alert.AlertType alertType, final List<ButtonType> buttons,
			final String headerBundleKey, final String contentBundleKey, final Object... contentParams) {
		final Alert alert = new Alert(alertType, String.format(bundle.getString(contentBundleKey), contentParams),
				buttons.toArray(new ButtonType[buttons.size()]));
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		this.register(alert);
		if (!headerBundleKey.isEmpty()) {
			alert.setHeaderText(bundle.getString(headerBundleKey));
		}
		return alert;
	}
	
	/**
	 * Create and initialize a new alert with the default buttons provided by
	 * JavaFX (OK and Cancel).
	 *
	 * @param alertType the alert type
	 * @param headerBundleKey the resource bundle key for the alert header
	 * text, or {@code ""} for the default header text provided by JavaFX
	 * @param contentBundleKey the resource bundle key for the alert content
	 * text, whose localized value may contain {@link Formatter}-style
	 * placeholders
	 * @param contentParams the objects to insert into the placeholders in
	 * {@code contentBundleKey}'s localized value
	 * @return a new alert
	 */
	public Alert makeAlert(final Alert.AlertType alertType, final String headerBundleKey, final String contentBundleKey,
			final Object... contentParams) {
		return makeAlert(alertType, new ArrayList<>(), headerBundleKey, contentBundleKey, contentParams);
	}

	/**
	 * Create and initialize an {@link ExceptionAlert}.
	 *
	 * @param exc the {@link Throwable} for which to make an alert
	 * @param contentBundleKey the resource bundle key for the alert content
	 * text, whose localized value may contain {@link Formatter}-style
	 * placeholders
	 * @param contentParams the objects to insert into the placeholders in
	 * {@code contentBundleKey}'s localized value
	 * @return a new exception alert
	 */
	public Alert makeExceptionAlert(final Throwable exc, final String contentBundleKey, final Object... contentParams) {
		return new ExceptionAlert(this.injector, String.format(bundle.getString(contentBundleKey), contentParams), exc);
	}

	/**
	 * Create and initialize an {@link ExceptionAlert}.
	 *
	 * @param exc the {@link Throwable} for which to make an alert
	 * @param headerBundleKey the resource bundle key for the alert header
	 * text, or {@code ""} for the default header text provided by JavaFX
	 * @param contentBundleKey the resource bundle key for the alert content
	 * text, whose localized value may contain {@link Formatter}-style
	 * placeholders
	 * @param contentParams the objects to insert into the placeholders in
	 * {@code contentBundleKey}'s localized value
	 * @return a new exception alert
	 */
	public Alert makeExceptionAlert(final Throwable exc, final String headerBundleKey, final String contentBundleKey,
			final Object... contentParams) {
		Alert alert = makeExceptionAlert(exc, contentBundleKey, contentParams);
		if (!headerBundleKey.isEmpty()) {
			alert.setHeaderText(bundle.getString(headerBundleKey));
		}
		return alert;
	}
	
	/**
	 * A read-only property containing the currently focused stage. If a non-JavaFX
	 * window or an unregistered stage is in focus, the property's value is
	 * {@code null}.
	 *
	 * @return a property containing the currently focused stage
	 */
	public ReadOnlyObjectProperty<Stage> currentProperty() {
		return this.current;
	}

	/**
	 * Get the currently focused stage. If a non-JavaFX window or an unregistered
	 * stage is in focus, this method returns {@code null}.
	 *
	 * @return the currently focused stage
	 */
	public Stage getCurrent() {
		return this.currentProperty().get();
	}

	/**
	 * Get the main stage, as previously set using
	 * {@link #registerMainStage(Stage, String)}.
	 * 
	 * @return the main stage
	 */
	public Stage getMainStage() {
		return this.mainStage;
	}

	/**
	 * Get a read-only set containing all registered stages. The returned set should
	 * not be permanently stored or copied elsewhere, as this would prevent all
	 * registered stages from being garbage-collected.
	 * 
	 * @return a read-only set containing all registered stages
	 */
	public Set<Stage> getRegistered() {
		return Collections.unmodifiableSet(this.registered.keySet());
	}

	/**
	 * Get the persistence ID of the given stage.
	 * 
	 * @param stage the stage for which to get the persistence ID
	 * @return the stage's persistence ID, or {@code null} if none
	 */
	public static String getPersistenceID(final Stage stage) {
		Objects.requireNonNull(stage);

		return (String) stage.getProperties().get(PropertiesKey.PERSISTENCE_ID);
	}

	/**
	 * Set the given stage's persistence ID.
	 * 
	 * @param stage the stage for which to set the persistence ID
	 * @param persistenceID the persistence ID to set, or {@code null} to
	 * remove it
	 */
	public static void setPersistenceID(final Stage stage, final String persistenceID) {
		Objects.requireNonNull(stage);

		if (persistenceID == null) {
			stage.getProperties().remove(PropertiesKey.PERSISTENCE_ID);
		} else {
			stage.getProperties().put(PropertiesKey.PERSISTENCE_ID, persistenceID);
		}
	}

	/**
	 * Get whether the given stage uses the global Mac menu bar. On non-Mac systems
	 * this setting should not be used.
	 * 
	 * @param stage the stage for which to get this setting
	 * @return whether the given stage uses the global Mac menu bar, or
	 * {@code false} if not set
	 */
	public static boolean isUseGlobalMacMenuBar(final Stage stage) {
		return (boolean) stage.getProperties().getOrDefault(PropertiesKey.USE_GLOBAL_MAC_MENU_BAR, false);
	}

	/**
	 * On Mac, set the given stage's menu bar. On other systems this method does
	 * nothing.
	 *
	 * @param menuBar the menu bar to use, or {@code null} to use the global
	 * menu bar
	 */
	public void setMacMenuBar(final Stage stage, final MenuBar menuBar) {
		Objects.requireNonNull(stage);

		if (this.menuToolkit == null) {
			return;
		}

		stage.getProperties().put(PropertiesKey.USE_GLOBAL_MAC_MENU_BAR, menuBar == null);
		final Scene scene = stage.getScene();
		if (scene != null) {
			final Parent root = scene.getRoot();
			if (root instanceof Pane) {
				final ObservableList<Menu> globalMenus = this.globalMacMenuBar.getMenus();
				if (menuBar != null) {
					// Temporary placeholder for the application menu, is later replaced with the
					// global application menu
					menuBar.getMenus().add(0, new Menu("Invisible Application Menu"));
					// Add the Window and Help menus from the global menu bar
					menuBar.getMenus().addAll(globalMenus.subList(globalMenus.size()-2, globalMenus.size()));
				}
				this.menuToolkit.setMenuBar((Pane) root, menuBar == null ? this.globalMacMenuBar : menuBar);
				// Put the application menu from the global menu bar back
				menuToolkit.setApplicationMenu(globalMenus.get(0));
			}
		}
	}

	/**
	 * <p>On Mac, set the given menu bar as the menu bar for all registered
	 * stages and any stages registered in the future. On other systems this
	 * method does nothing.</p>
	 * 
	 * <p>This method is similar to
	 * {@link MenuToolkit#setGlobalMenuBar(MenuBar)}, except that it only
	 * affects registered stages and handles stages with a {@code null} scene
	 * correctly.</p>
	 * 
	 * @param menuBar the menu bar to set
	 */
	public void setGlobalMacMenuBar(final MenuBar menuBar) {
		if (this.menuToolkit == null) {
			return;
		}

		this.globalMacMenuBar = menuBar;
		this.getRegistered().stream().filter(StageManager::isUseGlobalMacMenuBar)
				.forEach(stage -> this.setMacMenuBar(stage, null));
	}

}
