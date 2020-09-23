package de.prob2.ui.preferences;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ProBPreference;
import de.prob.exception.ProBError;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.TabPersistenceHandler;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.ProjectManager;

import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
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
		new Locale("ru"),
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

	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final ResourceBundle bundle;
	private final ProjectManager projectManager;
	private final CurrentProject currentProject;
	private final UIState uiState;
	private final GlobalPreferences globalPreferences;
	private final ProBPreferences globalProBPrefs;
	private final Config config;
	private TabPersistenceHandler tabPersistenceHandler;

	@Inject
	private PreferencesStage(
		final StageManager stageManager,
		final FileChooserManager fileChooserManager,
		final ResourceBundle bundle,
		final ProjectManager projectManager,
		final CurrentProject currentProject,
		final UIState uiState,
		final GlobalPreferences globalPreferences,
		final ProBPreferences globalProBPrefs,
		final Config config,
		final MachineLoader machineLoader
	) {
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.bundle = bundle;
		this.projectManager = projectManager;
		this.currentProject = currentProject;
		this.uiState = uiState;
		this.globalPreferences = globalPreferences;
		this.globalProBPrefs = globalProBPrefs;
		this.config = config;
		this.globalProBPrefs.setStateSpace(machineLoader.getEmptyStateSpace());

		stageManager.loadFXML(this, "preferences_stage.fxml");
	}

	@FXML
	public void initialize() {
		// General	
		final SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50);
		
		// bindBidirectional doesn't work properly here, don't ask why
		this.projectManager.maximumRecentProjectsProperty().addListener((observable, from, to) -> valueFactory.setValue((Integer)to));
		valueFactory.valueProperty().addListener((observable, from, to) -> this.projectManager.setMaximumRecentProjects(to));
		valueFactory.setValue(this.projectManager.getMaximumRecentProjects());

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

		this.globalPrefsView.setPreferences(this.globalProBPrefs);

		this.undoButton.disableProperty().bind(this.globalProBPrefs.changesAppliedProperty());
		this.applyWarning.visibleProperty().bind(this.globalProBPrefs.changesAppliedProperty().not());
		this.applyButton.disableProperty().bind(this.globalProBPrefs.changesAppliedProperty());

		// prevent text on buttons from being abbreviated
		undoButton.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		applyButton.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		resetButton.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

		this.tabPersistenceHandler = new TabPersistenceHandler(tabPane);
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.currentPreference != null) {
					getTabPersistenceHandler().setCurrentTab(configData.currentPreference);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.currentPreference = getTabPersistenceHandler().getCurrentTab();
			}
		});
	}

	@FXML
	private void selectDefaultLocation() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle(bundle.getString("preferences.stage.tabs.general.directoryChooser.selectLocation.title"));
		dirChooser.setInitialDirectory(new File(defaultLocationField.getText()));
		final Path path = fileChooserManager.showDirectoryChooser(dirChooser, null, this.getOwner());
		
		if (path != null) {
			defaultLocationField.setText(path.toString());
		}
	}

	@FXML
	private void handleClose() {
		this.hide();
	}

	@FXML
	private void handleUndoChanges() {
		this.globalProBPrefs.rollback();
		this.globalPrefsView.refresh();
	}

	@FXML
	private void handleRestoreDefaults() {
		this.globalProBPrefs.restoreDefaults();
		this.globalPrefsView.refresh();
	}
	
	@FXML
	private void handleApply() {
		final Map<String, String> changed = new HashMap<>(this.globalProBPrefs.getChangedPreferences());
		
		try {
			this.globalProBPrefs.apply();
		} catch (final ProBError e) {
			LOGGER.info("Failed to apply preference changes (this is probably because of invalid preference values entered by the user, and not a bug)", e);
			final Alert alert = stageManager.makeExceptionAlert(e, "preferences.stage.tabs.globalPreferences.alerts.failedToAppyChanges.content");
			alert.initOwner(this);
			alert.show();
		}
		
		final Map<String, ProBPreference> defaults = this.globalProBPrefs.getPreferences();
		for (final Map.Entry<String, String> entry : changed.entrySet()) {
			if (defaults.get(entry.getKey()).defaultValue.equals(entry.getValue())) {
				this.globalPreferences.remove(entry.getKey());
			} else {
				this.globalPreferences.put(entry.getKey(), entry.getValue());
			}
		}
		
		if (this.currentProject.getCurrentMachine() != null) {
			this.currentProject.reloadCurrentMachine();
		}
	}
	
	public TabPersistenceHandler getTabPersistenceHandler() {
		return tabPersistenceHandler;
	}

}
