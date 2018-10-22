package de.prob2.ui.preferences;

import java.io.File;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.exception.ProBError;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.RecentProjects;
import de.prob2.ui.persistence.TabPersistenceHandler;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;

import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class PreferencesStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesStage.class);
	// The locales to show in the language selection menu. This needs to be updated when new translations are added.
	private static final Locale[] SUPPORTED_LOCALES = {
		null, // system default
		Locale.ENGLISH,
		Locale.FRENCH,
		Locale.GERMAN,
		new Locale("pt"),
	};

	@FXML private Spinner<Integer> recentProjectsCountSpinner;
	@FXML private TextField defaultLocationField;
	@FXML private ChoiceBox<Locale> localeOverrideBox;
	@FXML private PreferencesView globalPrefsView;
	@FXML private Button undoButton;
	@FXML private Button resetButton;
	@FXML private Button applyButton;
	@FXML private Label applyWarning;
	@FXML private TabPane tabPane;

	private final GlobalPreferences globalPreferences;
	private final ProBPreferences globalProBPrefs;
	private final RecentProjects recentProjects;
	private final ResourceBundle bundle;
	private final CurrentProject currentProject;
	private final UIState uiState;
	private final PreferencesHandler preferencesHandler;
	private final TabPersistenceHandler tabPersistenceHandler;

	@Inject
	private PreferencesStage(
		final GlobalPreferences globalPreferences,
		final ProBPreferences globalProBPrefs,
		final MachineLoader machineLoader,
		final RecentProjects recentProjects,
		final StageManager stageManager,
		final ResourceBundle bundle,
		final CurrentProject currentProject,
		final UIState uiState,
		final PreferencesHandler preferencesHandler
	) {
		this.globalPreferences = globalPreferences;
		this.globalProBPrefs = globalProBPrefs;
		this.globalProBPrefs.setStateSpace(machineLoader.getEmptyStateSpace());
		this.recentProjects = recentProjects;
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.uiState = uiState;

		stageManager.loadFXML(this, "preferences_stage.fxml");
		this.preferencesHandler = preferencesHandler;
		this.tabPersistenceHandler = new TabPersistenceHandler(tabPane);
	}

	@FXML
	public void initialize() {
		// General	
		final SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50);
		
		// bindBidirectional doesn't work properly here, don't ask why
		this.recentProjects.maximumProperty().addListener((observable, from, to) -> valueFactory.setValue((Integer)to));
		valueFactory.valueProperty().addListener((observable, from, to) -> this.recentProjects.setMaximum(to));
		valueFactory.setValue(this.recentProjects.getMaximum());

		this.recentProjectsCountSpinner.setValueFactory(valueFactory);

		defaultLocationField.setText(this.currentProject.getDefaultLocation().toString());
		defaultLocationField.textProperty().addListener((observable, from, to) -> this.currentProject.setDefaultLocation(Paths.get(to)));

		localeOverrideBox.valueProperty().bindBidirectional(uiState.localeOverrideProperty());
		localeOverrideBox.setConverter(new StringConverter<Locale>() {
			@Override
			public String toString(Locale object) {
				return object == null ? "System Default" : object.getDisplayName(object);
			}

			@Override
			public Locale fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to Locale not supported");
			}
		});
		localeOverrideBox.getItems().setAll(SUPPORTED_LOCALES);

		// Global Preferences

		this.globalPrefsView.setPreferences(this.globalProBPrefs);

		this.globalPreferences.addListener((InvalidationListener) observable -> {
			for (final Map.Entry<String, String> entry : this.globalPreferences.entrySet()) {
				this.globalProBPrefs.setPreferenceValue(entry.getKey(), entry.getValue());
			}

			try {
				this.globalProBPrefs.apply();
			} catch (final ProBError e) {
				LOGGER.warn("Ignoring global preference changes because of exception", e);
			}
		});
		this.globalPreferences.addListener((MapChangeListener<String, String>) change -> {
			if (change.wasRemoved() && !change.wasAdded()) {
				this.globalProBPrefs.setPreferenceValue(change.getKey(), this.globalProBPrefs.getPreferences().get(change.getKey()).defaultValue);
				this.globalProBPrefs.apply();
			}
		});

		this.undoButton.disableProperty().bind(this.globalProBPrefs.changesAppliedProperty());
		this.applyWarning.visibleProperty().bind(this.globalProBPrefs.changesAppliedProperty().not());
		this.applyButton.disableProperty().bind(this.globalProBPrefs.changesAppliedProperty());
		
		// prevent text on buttons from being abbreviated
		undoButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
		applyButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
		resetButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
	}

	@FXML
	private void selectDefaultLocation() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle(bundle.getString("preferences.stage.tabs.general.directoryChooser.selectLocation.title"));
		dirChooser.setInitialDirectory(new File(defaultLocationField.getText()));
		File file = dirChooser.showDialog(this.getOwner());
		
		if (file != null) {
			defaultLocationField.setText(file.getAbsolutePath());
		}
	}

	@FXML
	private void handleUndoChanges() {
		preferencesHandler.undo();
	}

	@FXML
	private void handleRestoreDefaults() {
		preferencesHandler.restoreDefaults();
	}
	
	@FXML
	private void handleApply() {
		preferencesHandler.apply();
	}

	@FXML
	private void handleClose() {
		this.hide();
	}

	public TabPersistenceHandler getTabPersistenceHandler() {
		return tabPersistenceHandler;
	}

}
