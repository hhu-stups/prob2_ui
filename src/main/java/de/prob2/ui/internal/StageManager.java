package de.prob2.ui.internal;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
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
import de.prob2.ui.error.ExceptionAlert;
import de.prob2.ui.layout.FontSize;
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
 * Tracks registered stages to implement UI persistence and the Mac Cmd+W
 * shortcut. Also provides some convenience methods for creating {@link Stage}s
 * and {@link Alert}s and loading FXML files.
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
	 * Load a FXML file with {@code controller} as the root and controller.
	 * {@code filename} may be absolute, or relative to {@code controller}'s
	 * package.
	 *
	 * @param controller
	 *            the root and controller to use
	 * @param filename
	 *            the FXML file to load
	 */
	public void loadFXML(final Object controller, final String filename) {
		final FXMLLoader loader = injector.getInstance(FXMLLoader.class);
		if (!filename.startsWith("custom")) {
			loader.setLocation(controller.getClass().getResource(filename));
		} else {
			try {
				loader.setLocation(new URL(filename.replace("custom ", "")));
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			}
		}
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
	 * Load a FXML file using {@link #loadFXML(Object, String)} and register its
	 * root/controller using {@link #register(Stage, String)}.
	 *
	 * @param controller
	 *            the root and controller to use
	 * @param filename
	 *            the FXML file to load
	 * @param id
	 *            a string identifying the stage for UI persistence, or null if the
	 *            stage should not be persisted
	 */
	public void loadFXML(final Stage controller, final String filename, final String id) {
		this.loadFXML((Object) controller, filename);
		this.register(controller, id);
	}

	/**
	 * Load a FXML file using {@link #loadFXML(Stage, String, String)} with a
	 * {@code null} ID (persistence disabled).
	 *
	 * @param controller
	 *            the root and controller to use
	 * @param filename
	 *            the FXML file to load
	 */
	public void loadFXML(final Stage controller, final String filename) {
		this.loadFXML(controller, filename, null);
	}

	/**
	 * Register the given stage with the manager. The stage must already have its
	 * scene {@link Stage#setScene(Scene) set}. This method applies the
	 * {@link #STYLESHEET} to the stage's scene, sets the stage icon, and adds some
	 * internal listeners for implementing UI persistence and the Mac Cmd+W
	 * shortcut.
	 *
	 * @param stage
	 *            the stage to register
	 * @param id
	 *            a string identifying the stage for UI persistence, or null if the
	 *            stage should not be persisted
	 */
	public void register(final Stage stage, final String id) {
		this.registered.put(stage, null);
		setPersistenceID(stage, id);
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
						() -> region.minWidth(-1),
						region.minWidthProperty(),
						region.widthProperty()
					).add(this.stageSceneWidthDifference)
				);
				stage.minHeightProperty().bind(
					Bindings.createDoubleBinding(
						() -> region.minHeight(-1),
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

	public void registerMainStage(Stage primaryStage, String name) {
		this.mainStage = primaryStage;
		primaryStage.setOnShown(event -> {
			this.stageSceneWidthDifference.set(primaryStage.getWidth() - primaryStage.getScene().getWidth());
			this.stageSceneHeightDifference.set(primaryStage.getHeight() - primaryStage.getScene().getHeight());
			primaryStage.setOnShown(null);
		});
		this.register(primaryStage, name);
	}

	/**
	 * Create with the given {@link Scene} as its scene, and register it
	 * automatically.
	 *
	 * @param scene
	 *            the new scene's stage
	 * @param id
	 *            a string identifying the stage for UI persistence, or null if the
	 *            stage should not be persisted
	 * @return a new stage with the given scene
	 */
	public Stage makeStage(final Scene scene, final String id) {
		final Stage stage = new Stage();
		stage.setScene(scene);
		this.register(stage, id);
		return stage;
	}

	/**
	 * Register the given dialog with the manager. Currently this only applies the
	 * {@link #STYLESHEET} to the dialog's dialog pane.
	 *
	 * @param dialog
	 *            the dialog to register
	 */
	public void register(final Dialog<?> dialog) {
		dialog.getDialogPane().getStylesheets().add(STYLESHEET);
	}

	/**
	 * Create and register a new alert.
	 *
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
	
	public Alert makeAlert(final Alert.AlertType alertType, final String headerBundleKey, final String contentBundleKey,
			final Object... contentParams) {
		return makeAlert(alertType, new ArrayList<>(), headerBundleKey, contentBundleKey, contentParams);
	}

	public Alert makeExceptionAlert(final Throwable exc, final String contentBundleKey, final Object... contentParams) {
		return new ExceptionAlert(this.injector, String.format(bundle.getString(contentBundleKey), contentParams), exc);
	}

	public Alert makeExceptionAlert(final Throwable exc, final String headerBundleKey, final String contentBundleKey,
			final Object... contentParams) {
		Alert alert = makeExceptionAlert(exc, contentBundleKey, contentParams);
		if (!headerBundleKey.isEmpty()) {
			alert.setHeaderText(bundle.getString(headerBundleKey));
		}
		return alert;
	}
	
	/**
	 * Get the extension of the given file name.
	 * 
	 * @param filename the file name for which to get the extension
	 * @return the file extension
	 */
	public static String getExtension(final String filename) {
		final String[] parts = filename.split("\\.");
		return parts[parts.length-1];
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
	 * Get the main stage.
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
	 * @param stage
	 *            the stage for which to get the persistence ID
	 * @return the stage's persistence ID, or {@code null} if none
	 */
	public static String getPersistenceID(final Stage stage) {
		Objects.requireNonNull(stage);

		return (String) stage.getProperties().get(PropertiesKey.PERSISTENCE_ID);
	}

	/**
	 * Set the given stage's persistence ID.
	 * 
	 * @param stage
	 *            the stage for which to set the persistence ID
	 * @param id
	 *            the persistence ID to set, or {@code null} to remove it
	 */
	public static void setPersistenceID(final Stage stage, final String id) {
		Objects.requireNonNull(stage);

		if (id == null) {
			stage.getProperties().remove(PropertiesKey.PERSISTENCE_ID);
		} else {
			stage.getProperties().put(PropertiesKey.PERSISTENCE_ID, id);
		}
	}

	/**
	 * Get whether the given stage uses the global Mac menu bar. On non-Mac systems
	 * this setting should not be used.
	 * 
	 * @param stage
	 *            the stage for which to get this setting
	 * @return whether the given stage uses the global Mac menu bar, or
	 *         {@code false} if not set
	 */
	public static boolean isUseGlobalMacMenuBar(final Stage stage) {
		return (boolean) stage.getProperties().getOrDefault(PropertiesKey.USE_GLOBAL_MAC_MENU_BAR, false);
	}

	/**
	 * On Mac, set the given stage's menu bar. On other systems this method does
	 * nothing.
	 *
	 * @param menuBar
	 *            the menu bar to use, or {@code null} to use the global menu bar
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
	 * <p>
	 * On Mac, set the given menu bar as the menu bar for all registered stages and
	 * any stages registered in the future. On other systems this method does
	 * nothing.
	 * </p>
	 * <p>
	 * This method is similar to {@link MenuToolkit#setGlobalMenuBar(MenuBar)},
	 * except that it only affects registered stages and handles stages with a null
	 * scene correctly.
	 * </p>
	 * 
	 * @param menuBar
	 *            the menu bar to set
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
