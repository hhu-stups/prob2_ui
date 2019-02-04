package de.prob2.ui.preferences;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.RecentProjects;
import de.prob2.ui.persistence.TabPersistenceHandler;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;



@Singleton
public final class PreferencesStage extends AbstractPreferencesStage {
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
	@FXML private TabPane tabPane;

	private final RecentProjects recentProjects;
	private final ResourceBundle bundle;
	private final CurrentProject currentProject;
	private final UIState uiState;
	private final Config config;
	private TabPersistenceHandler tabPersistenceHandler;

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
		final PreferencesHandler preferencesHandler,
		final Config config
	) {
		super(globalProBPrefs, globalPreferences, preferencesHandler, machineLoader);
		this.recentProjects = recentProjects;
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.uiState = uiState;
		this.config = config;

		stageManager.loadFXML(this, "preferences_stage.fxml");
	}

	@Override
	@FXML
	public void initialize() {
		super.initialize();
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

		this.globalPrefsView.setPreferences(this.globalProBPrefs);

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
		File file = dirChooser.showDialog(this.getOwner());
		
		if (file != null) {
			defaultLocationField.setText(file.getAbsolutePath());
		}
	}

	@FXML
	private void handleClose() {
		this.hide();
	}

	public TabPersistenceHandler getTabPersistenceHandler() {
		return tabPersistenceHandler;
	}

}
