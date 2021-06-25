package de.prob2.ui.preferences;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.ProBPreference;
import de.prob.exception.ProBError;
import de.prob.statespace.StateSpace;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.ErrorDisplayFilter;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.PersistenceUtils;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.ProjectManager;

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
		new Locale("ru"),
	};

	private static final Map<ErrorItem.Type, String> ERROR_LEVEL_DESCRIPTION_KEYS;
	static {
		final Map<ErrorItem.Type, String> errorLevelDescriptionKeys = new EnumMap<>(ErrorItem.Type.class);
		errorLevelDescriptionKeys.put(ErrorItem.Type.ERROR, "preferences.stage.tabs.general.errorLevels.error");
		errorLevelDescriptionKeys.put(ErrorItem.Type.WARNING, "preferences.stage.tabs.general.errorLevels.warning");
		errorLevelDescriptionKeys.put(ErrorItem.Type.MESSAGE, "preferences.stage.tabs.general.errorLevels.message");
		ERROR_LEVEL_DESCRIPTION_KEYS = Collections.unmodifiableMap(errorLevelDescriptionKeys);
	}

	@FXML private ChoiceBox<ErrorItem.Type> errorLevelChoiceBox;
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
	private final ErrorDisplayFilter errorDisplayFilter;
	private final ProjectManager projectManager;
	private final CurrentProject currentProject;
	private final UIState uiState;
	private final GlobalPreferences globalPreferences;
	private final PreferencesChangeState globalPrefsChangeState;
	private final Config config;
	private final StateSpace emptyStateSpace;

	@Inject
	private PreferencesStage(
		final StageManager stageManager,
		final FileChooserManager fileChooserManager,
		final ResourceBundle bundle,
		final ErrorDisplayFilter errorDisplayFilter,
		final ProjectManager projectManager,
		final CurrentProject currentProject,
		final UIState uiState,
		final GlobalPreferences globalPreferences,
		final Config config,
		final MachineLoader machineLoader
	) {
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.bundle = bundle;
		this.errorDisplayFilter = errorDisplayFilter;
		this.projectManager = projectManager;
		this.currentProject = currentProject;
		this.uiState = uiState;
		this.globalPreferences = globalPreferences;
		this.emptyStateSpace = machineLoader.getEmptyStateSpace();
		this.globalPrefsChangeState = new PreferencesChangeState(this.emptyStateSpace.getPreferenceInformation());
		this.config = config;
		
		// Update globalPrefsChangeState when globalPreferences changes
		this.globalPreferences.addListener((o, from, to) -> this.globalPrefsChangeState.setCurrentPreferenceValues(to));
		this.globalPrefsChangeState.setCurrentPreferenceValues(this.globalPreferences);

		stageManager.loadFXML(this, "preferences_stage.fxml");
	}

	@FXML
	public void initialize() {
		// General
		this.errorLevelChoiceBox.getItems().setAll(ErrorItem.Type.values());
		Collections.reverse(this.errorLevelChoiceBox.getItems());
		// Require at least ErrorItem.Type.ERROR to be always visible.
		this.errorLevelChoiceBox.getItems().remove(ErrorItem.Type.INTERNAL_ERROR);
		this.errorLevelChoiceBox.valueProperty().bindBidirectional(this.errorDisplayFilter.errorLevelProperty());
		this.errorLevelChoiceBox.setConverter(new StringConverter<ErrorItem.Type>() {
			@Override
			public String toString(final ErrorItem.Type object) {
				return bundle.getString(ERROR_LEVEL_DESCRIPTION_KEYS.get(object));
			}
			
			@Override
			public ErrorItem.Type fromString(final String string) {
				throw new UnsupportedOperationException("Conversion from String to ErrorItem.Type not supported");
			}
		});
		
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

		this.globalPrefsView.setState(this.globalPrefsChangeState);

		this.undoButton.disableProperty().bind(this.globalPrefsChangeState.changesAppliedProperty());
		this.applyWarning.visibleProperty().bind(this.globalPrefsChangeState.changesAppliedProperty().not());
		this.applyButton.disableProperty().bind(this.globalPrefsChangeState.changesAppliedProperty());

		// prevent text on buttons from being abbreviated
		undoButton.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		applyButton.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		resetButton.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.currentPreference != null) {
					PersistenceUtils.setCurrentTab(tabPane, configData.currentPreference);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.currentPreference = PersistenceUtils.getCurrentTab(tabPane);
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
		this.globalPrefsChangeState.rollback();
		this.globalPrefsView.refresh();
	}

	@FXML
	private void handleRestoreDefaults() {
		this.globalPrefsChangeState.restoreDefaults();
		this.globalPrefsView.refresh();
	}
	
	@FXML
	private void handleApply() {
		final Map<String, String> changed = new HashMap<>(this.globalPrefsChangeState.getPreferenceChanges());
		
		try {
			this.emptyStateSpace.changePreferences(changed);
		} catch (final ProBError e) {
			LOGGER.info("Failed to apply preference changes (this is probably because of invalid preference values entered by the user, and not a bug)", e);
			final Alert alert = stageManager.makeExceptionAlert(e, "preferences.stage.tabs.globalPreferences.alerts.failedToAppyChanges.content");
			alert.initOwner(this);
			alert.show();
		}
		
		this.globalPrefsChangeState.apply();
		
		final Map<String, ProBPreference> defaults = this.globalPrefsChangeState.getPreferenceInfos();
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
}
