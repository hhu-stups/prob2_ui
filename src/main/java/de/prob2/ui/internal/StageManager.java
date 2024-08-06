package de.prob2.ui.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.jangassen.MenuToolkit;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.error.ExceptionAlert;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.persistence.UIPersistence;
import de.prob2.ui.persistence.UIState;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		USE_GLOBAL_MAC_MENU_BAR
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(StageManager.class);

	private static final String STYLESHEET = "prob.css";
	public static final Image ICON = new Image(StageManager.class.getResource("/de/prob2/ui/ProB_Icon.png").toExternalForm());

	private final Provider<FXMLLoader> loaderProvider;
	private final FontSize fontSize;
	private final Provider<ExceptionAlert> exceptionAlertProvider;
	private final MenuToolkit menuToolkit;
	private final UIState uiState;
	private final I18n i18n;

	private final ObjectProperty<Stage> current;
	private final Map<Stage, Void> registered;
	private MenuBar globalMacMenuBar;
	private Stage mainStage;

	@Inject
	private StageManager(
		Provider<FXMLLoader> loaderProvider,
		FontSize fontSize,
		Provider<ExceptionAlert> exceptionAlertProvider,
		@Nullable MenuToolkit menuToolkit,
		UIState uiState,
		I18n i18n
	) {
		this.loaderProvider = loaderProvider;
		this.fontSize = fontSize;
		this.exceptionAlertProvider = exceptionAlertProvider;
		this.menuToolkit = menuToolkit;
		this.uiState = uiState;
		this.i18n = i18n;

		this.current = new SimpleObjectProperty<>(this, "current");
		this.registered = new WeakHashMap<>();
		this.globalMacMenuBar = null;
	}

	private static Node getRootNode(Object fxmlRoot) {
		if (fxmlRoot instanceof Node) {
			return (Node)fxmlRoot;
		} else if (fxmlRoot instanceof Scene) {
			return ((Scene)fxmlRoot).getRoot();
		} else if (fxmlRoot instanceof Window) {
			Scene scene = ((Window)fxmlRoot).getScene();
			return scene == null ? null : scene.getRoot();
		} else if (fxmlRoot instanceof Dialog<?>) {
			return ((Dialog<?>)fxmlRoot).getDialogPane();
		} else {
			return null;
		}
	}

	/**
	 * Load an FXML file with {@code controller} as the root and controller.
	 *
	 * @param controller the object to use as the FXML file's root
	 * and controller
	 * @param fxmlUrl the URL of the FXML file to load
	 */
	public void loadFXML(final Object controller, final URL fxmlUrl) {
		Objects.requireNonNull(controller, "controller");
		Objects.requireNonNull(fxmlUrl, "fxmlUrl");

		LOGGER.trace("Begin loading FXML from {} with controller {}", fxmlUrl, controller.getClass());

		final FXMLLoader loader = loaderProvider.get();
		loader.setLocation(fxmlUrl);
		loader.setRoot(controller);
		loader.setController(controller);
		if (controller instanceof Dialog<?>) {
			this.register((Dialog<?>)controller);
		}
		try {
			loader.load();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Node rootNode = getRootNode(controller);
		if (rootNode != null) {
			fontSize.applyTo(rootNode);
		}

		LOGGER.trace("End loading FXML from {} with controller {}", fxmlUrl, controller);
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
		final URL fxmlUrl = controller.getClass().getResource(fxmlResource);
		if (fxmlUrl == null) {
			throw new IllegalArgumentException("Resource not found: " + fxmlResource);
		}
		this.loadFXML(controller, fxmlUrl);
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
	 * @param stageId a string identifying the stage for UI persistence,
	 * or {@code null} if the stage should not be persisted
	 */
	public void register(final Stage stage, final String stageId) {
		this.registered.put(stage, null);
		stage.getProperties().putIfAbsent(PropertiesKey.USE_GLOBAL_MAC_MENU_BAR, true);
		stage.getScene().getStylesheets().add(STYLESHEET);
		stage.getIcons().add(ICON);

		if (stageId != null) {
			stage.focusedProperty().addListener((o, from, to) -> {
				if (to) {
					uiState.moveStageToEnd(stageId);
				}
			});

			// Use the onShowing/onHiding event handlers instead of a listener on the showing property
			// because the event handlers are called earlier than the listeners.
			// This avoids the windows visibly jumping around when their position is being restored.
			stage.setOnShowing(event -> {
				BoundingBox box = uiState.getSavedStageBoxes().get(stageId);
				if (box != null) {
					if (!Screen.getScreensForRectangle(box.getMinX(), box.getMinY(), box.getWidth(), box.getHeight()).isEmpty()) {
						LOGGER.trace(
							"Restoring saved position/size for stage with ID \"{}\": x={}, y={}, width={}, height={}",
							stageId, box.getMinX(), box.getMinY(), box.getWidth(), box.getHeight()
						);
						stage.setX(box.getMinX());
						stage.setY(box.getMinY());
						stage.setWidth(box.getWidth());
						stage.setHeight(box.getHeight());
					} else {
						LOGGER.trace(
							"Not restoring saved position/size for stage with ID \"{}\" because it's offscreen: x={}, y={}, width={}, height={}",
							stageId, box.getMinX(), box.getMinY(), box.getWidth(), box.getHeight()
						);
					}
				}
				uiState.getStages().put(stageId, new WeakReference<>(stage));
				uiState.getSavedVisibleStages().add(stageId);
			});
			stage.setOnHiding(event -> {
				uiState.getSavedVisibleStages().remove(stageId);
				BoundingBox box = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
				LOGGER.trace(
					"Saving position/size for stage with ID \"{}\": x={}, y={}, width={}, height={}",
					stageId, box.getMinX(), box.getMinY(), box.getWidth(), box.getHeight()
				);
				uiState.getSavedStageBoxes().put(stageId, box);
			});
		}

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
		this.register(stage, persistenceID);
	}

	/**
	 * Initialize the given dialog.
	 *
	 * @param dialog the dialog to register
	 */
	public void register(final Dialog<?> dialog) {
		// Consider all dialogs as owned by the main stage by default.
		// Callers of register, makeAlert, etc. should replace this with a more accurate owner if possible,
		// so that dialogs created by non-main windows or detached views are owned by the correct window.
		// The owner of a dialog influences for example what screen the dialog appears on.
		// If no owner is set, the dialog appears on the user's primary screen,
		// which may be a different screen than the one with the window that created the dialog.
		dialog.initOwner(this.getMainStage());
		dialog.setResizable(true); // Necessary to make a registered dialog readable
		dialog.getDialogPane().getStylesheets().add(STYLESHEET);
	}

	/**
	 * Create and initialize a new alert with custom buttons.
	 *
	 * @param alertType the alert type
	 * @param buttons the custom buttons
	 * @param headerBundleKey the resource bundle key for the alert header
	 * text, or either {@code null} or {@code ""} for the default header text provided by JavaFX
	 * @param contentBundleKey the resource bundle key for the alert content
	 * text, whose localized value may contain {@link MessageFormat}-style
	 * placeholders
	 * @param contentParams the objects to insert into the placeholders in
	 * {@code contentBundleKey}'s localized value
	 * @return a new alert
	 */
	public Alert makeAlert(final Alert.AlertType alertType, final List<ButtonType> buttons,
			final String headerBundleKey, final String contentBundleKey, final Object... contentParams) {
		String content = contentBundleKey != null && !contentBundleKey.isEmpty() ? i18n.translate(contentBundleKey, contentParams) : "";
		final Alert alert = new Alert(alertType, content, buttons.toArray(new ButtonType[0]));
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		this.register(alert);
		if (headerBundleKey != null && !headerBundleKey.isEmpty()) {
			alert.setHeaderText(i18n.translate(headerBundleKey));
		}
		return alert;
	}
	
	/**
	 * Create and initialize a new alert with the default buttons provided by
	 * JavaFX (OK and Cancel).
	 *
	 * @param alertType the alert type
	 * @param headerBundleKey the resource bundle key for the alert header
	 * text, or either {@code null} or {@code ""} for the default header text provided by JavaFX
	 * @param contentBundleKey the resource bundle key for the alert content
	 * text, whose localized value may contain {@link MessageFormat}-style
	 * placeholders
	 * @param contentParams the objects to insert into the placeholders in
	 * {@code contentBundleKey}'s localized value
	 * @return a new alert
	 */
	public Alert makeAlert(final Alert.AlertType alertType, final String headerBundleKey, final String contentBundleKey,
			final Object... contentParams) {
		return makeAlert(alertType, Collections.emptyList(), headerBundleKey, contentBundleKey, contentParams);
	}

	/**
	 * Create and initialize an {@link ExceptionAlert}.
	 *
	 * @param exc the {@link Throwable} for which to make an alert
	 * @param contentBundleKey the resource bundle key for the alert content
	 * text, whose localized value may contain {@link MessageFormat}-style
	 * placeholders
	 * @param contentParams the objects to insert into the placeholders in
	 * {@code contentBundleKey}'s localized value
	 * @return a new exception alert
	 */
	public Alert makeExceptionAlert(final Throwable exc, final String contentBundleKey, final Object... contentParams) {
		final ExceptionAlert alert = exceptionAlertProvider.get();
		alert.setText(contentBundleKey != null && !contentBundleKey.isEmpty() ? i18n.translate(contentBundleKey, contentParams) : "");
		alert.setException(exc);
		this.register(alert);
		return alert;
	}

	/**
	 * Create and initialize an {@link ExceptionAlert}.
	 *
	 * @param exc the {@link Throwable} for which to make an alert
	 * @param headerBundleKey the resource bundle key for the alert header
	 * text, or either {@code null} or {@code ""} for the default header text provided by JavaFX
	 * @param contentBundleKey the resource bundle key for the alert content
	 * text, whose localized value may contain {@link MessageFormat}-style
	 * placeholders
	 * @param contentParams the objects to insert into the placeholders in
	 * {@code contentBundleKey}'s localized value
	 * @return a new exception alert
	 */
	public Alert makeExceptionAlert(final Throwable exc, final String headerBundleKey, final String contentBundleKey,
			final Object... contentParams) {
		Alert alert = makeExceptionAlert(exc, contentBundleKey, contentParams);
		if (headerBundleKey != null && !headerBundleKey.isEmpty()) {
			alert.setHeaderText(i18n.translate(headerBundleKey));
		}
		return alert;
	}
	
	public void showUnhandledExceptionAlert(Thread thread, Throwable exc, Window owner) {
		try {
			Alert alert = makeExceptionAlert(exc, "common.alerts.internalException.header", "common.alerts.internalException.content", thread);
			alert.initOwner(owner);
			alert.show();
		} catch (Throwable t) {
			LOGGER.error("An exception was thrown while handling an uncaught exception, something is really wrong!", t);
		}
	}
	
	public void showUnhandledExceptionAlert(Throwable exc, Window owner) {
		Thread thread = Thread.currentThread();
		Platform.runLater(() -> showUnhandledExceptionAlert(thread, exc, owner));
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
