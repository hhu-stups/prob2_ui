package de.prob2.ui.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob2.ui.MainController;
import de.prob2.ui.consoles.Console;
import de.prob2.ui.consoles.b.BConsole;
import de.prob2.ui.consoles.groovy.GroovyConsole;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.menu.MainView;
import de.prob2.ui.menu.RecentProjects;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.persistence.TablePersistenceHandler;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.plugin.ProBPluginManager;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.states.StatesView;
import de.prob2.ui.verifications.VerificationsView;

import javafx.geometry.BoundingBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class Config {
	/**
	 * A subset of the full {@link ConfigData}, which is used to load parts of the config before the injector is set up. This is needed to apply the locale override for example.
	 */
	private static class BasicConfigData {
		Locale localeOverride;

		private BasicConfigData() {}
	}

	/*
	 * The full set of config settings, used when the injector is available.
	 */
	private static final class ConfigData extends BasicConfigData {
		int maxRecentProjects;
		List<String> recentProjects;
		Console.ConfigData groovyConsoleSettings;
		Console.ConfigData bConsoleSettings;
		String guiState;
		List<String> visibleStages;
		Map<String, double[]> stageBoxes;
		List<String> groovyObjectTabs;
		String currentPreference;
		String currentMainTab;
		String currentVerificationTab;
		List<String> expandedTitledPanes;
		String defaultProjectLocation;
		double[] horizontalDividerPositions;
		double[] verticalDividerPositions;
		double[] statesViewColumnsWidth;
		String[] statesViewColumnsOrder;
		OperationsView.SortMode operationsSortMode;
		boolean operationsShowNotEnabled;
		Map<String, String> globalPreferences;
        private String pluginDirectory;

		private ConfigData() {}
	}

	private static final Charset CONFIG_CHARSET = Charset.forName("UTF-8");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final File LOCATION = new File(
			Main.getProBDirectory() + File.separator + "prob2ui" + File.separator + "config.json");

	private static final Logger logger = LoggerFactory.getLogger(Config.class);

	private final RecentProjects recentProjects;
	private final ConfigData defaultData;
	private final GroovyConsole groovyConsole;
	private final BConsole bConsole;
	private final Injector injector;
	private final UIState uiState;
	private final CurrentProject currentProject;
	private final GlobalPreferences globalPreferences;
	private final RuntimeOptions runtimeOptions;
	private final ProBPluginManager proBPluginManager;

	@Inject
	private Config(
		final RecentProjects recentProjects,
		final UIState uiState,
		final GroovyConsole groovyConsole,
		final BConsole bConsole,
		final Injector injector,
		final CurrentProject currentProject,
		final GlobalPreferences globalPreferences,
		final RuntimeOptions runtimeOptions,
		final StopActions stopActions,
        final ProBPluginManager proBPluginManager
	) {
		this.recentProjects = recentProjects;
		this.uiState = uiState;
		this.groovyConsole = groovyConsole;
		this.bConsole = bConsole;
		this.injector = injector;
		this.currentProject = currentProject;
		this.globalPreferences = globalPreferences;
		this.runtimeOptions = runtimeOptions;
		this.proBPluginManager = proBPluginManager;

		try (
			final InputStream is = Config.class.getResourceAsStream("default.json");
			final Reader defaultReader = new InputStreamReader(is, CONFIG_CHARSET)
		) {
			this.defaultData = GSON.fromJson(defaultReader, ConfigData.class);
		} catch (FileNotFoundException exc) {
			throw new IllegalStateException("Default config file not found", exc);
		} catch (IOException exc) {
			throw new IllegalStateException("Failed to open default config file", exc);
		}

		if (!LOCATION.getParentFile().exists() && !LOCATION.getParentFile().mkdirs()) {
			logger.warn("Failed to create the parent directory for the config file {}", LOCATION.getAbsolutePath());
		}

		this.load();
		
		stopActions.add(this::save);
	}

	/**
	 * Load basic settings from the config file. This method is static so it can be called before all of {@link Config}'s dependencies are available.
	 *
	 * @return basic settings from the config file
	 */
	private static BasicConfigData loadBasicConfig() {
		try (
			final InputStream is = new FileInputStream(LOCATION);
			final Reader reader = new InputStreamReader(is, CONFIG_CHARSET)
		) {
			return GSON.fromJson(reader, BasicConfigData.class);
		} catch (FileNotFoundException exc) {
			logger.info("Config file not found, while loading basic config, loading default settings", exc);
			return new BasicConfigData();
		} catch (IOException exc) {
			logger.warn("Failed to open config file while loading basic config", exc);
			return new BasicConfigData();
		}
	}

	/**
	 * Get the locale override from the config file. This method is static so it can be called before all of {@link Config}'s dependencies are available.
	 *
	 * @return the locale override
	 */
	public static Locale getLocaleOverride() {
		return loadBasicConfig().localeOverride;
	}

	private void replaceMissingWithDefaults(final ConfigData configData) {
		// If some keys are null (for example when loading a config from a
		// previous version that did not have those keys), replace them with
		// their values from the default config.
		if (configData.recentProjects == null) {
			configData.maxRecentProjects = this.defaultData.maxRecentProjects;
			configData.recentProjects = new ArrayList<>(this.defaultData.recentProjects);
		}
		if (configData.guiState == null || configData.guiState.isEmpty()) {
			configData.guiState = this.defaultData.guiState;
		}
		if (configData.visibleStages == null) {
			configData.visibleStages = new ArrayList<>(this.defaultData.visibleStages);
		}
		if (configData.stageBoxes == null) {
			configData.stageBoxes = new HashMap<>(this.defaultData.stageBoxes);
		}
		if (configData.groovyObjectTabs == null) {
			configData.groovyObjectTabs = new ArrayList<>(this.defaultData.groovyObjectTabs);
		}
		if (configData.currentPreference == null) {
			configData.currentPreference = this.defaultData.currentPreference;
		}
		if (configData.currentMainTab == null) {
			configData.currentMainTab = this.defaultData.currentMainTab;
		}
		if (configData.currentVerificationTab == null) {
			configData.currentVerificationTab = this.defaultData.currentVerificationTab;
		}
		if (configData.groovyConsoleSettings == null) {
			configData.groovyConsoleSettings = this.defaultData.groovyConsoleSettings;
		}
		if (configData.bConsoleSettings == null) {
			configData.bConsoleSettings = this.defaultData.bConsoleSettings;
		}
		if (configData.expandedTitledPanes == null) {
			configData.expandedTitledPanes = this.defaultData.expandedTitledPanes;
		}
		if (configData.defaultProjectLocation == null) {
			configData.defaultProjectLocation = System.getProperty("user.home");
		}

		MainController main = injector.getInstance(MainController.class);

		if (configData.horizontalDividerPositions == null) {
			configData.horizontalDividerPositions = main.getHorizontalDividerPositions();
		}

		if (configData.verticalDividerPositions == null) {
			configData.verticalDividerPositions = main.getVerticalDividerPositions();
		}

		if (configData.statesViewColumnsWidth == null) {
			configData.statesViewColumnsWidth = this.defaultData.statesViewColumnsWidth.clone();
		}

		if (configData.statesViewColumnsOrder == null) {
			configData.statesViewColumnsOrder = this.defaultData.statesViewColumnsOrder.clone();
		}

		if (configData.operationsSortMode == null) {
			configData.operationsSortMode = this.defaultData.operationsSortMode;
		}

		if (configData.globalPreferences == null) {
			configData.globalPreferences = new HashMap<>();
		}
	}

	public void load() {
		ConfigData configData;
		if (this.runtimeOptions.isLoadConfig()) {
			try (
				final InputStream is = new FileInputStream(LOCATION);
				final Reader reader = new InputStreamReader(is, CONFIG_CHARSET)
			) {
				configData = GSON.fromJson(reader, ConfigData.class);
			} catch (FileNotFoundException exc) {
				logger.info("Config file not found, loading default settings", exc);
				configData = this.defaultData;
			} catch (IOException exc) {
				logger.warn("Failed to open config file", exc);
				return;
			}
		} else {
			logger.info("Config loading disabled via runtime options, loading default config");
			configData = this.defaultData;
		}
		
		if(configData == null) {
			configData = this.defaultData;
		}
		
		this.replaceMissingWithDefaults(configData);

		this.uiState.setLocaleOverride(configData.localeOverride);

		this.recentProjects.setMaximum(configData.maxRecentProjects);
		this.recentProjects.setAll(configData.recentProjects);

		this.currentProject.setDefaultLocation(Paths.get(configData.defaultProjectLocation));

		this.uiState.setGuiState(configData.guiState);
		this.uiState.getSavedVisibleStages().clear();
		this.uiState.getSavedVisibleStages().addAll(configData.visibleStages);

		for (final Map.Entry<String, double[]> entry : configData.stageBoxes.entrySet()) {
			final double[] v = entry.getValue();
			this.uiState.getSavedStageBoxes().put(entry.getKey(), new BoundingBox(v[0], v[1], v[2], v[3]));
		}

		configData.groovyObjectTabs.forEach(this.uiState::addGroovyObjectTab);

		for (String pane : configData.expandedTitledPanes) {
			this.uiState.getExpandedTitledPanes().add(pane);
		}
		

		this.injector.getInstance(PreferencesStage.class).getTabPersistenceHandler().setCurrentTab(configData.currentPreference);
		this.injector.getInstance(MainView.class).getTabPersistenceHandler().setCurrentTab(configData.currentMainTab);
		this.injector.getInstance(VerificationsView.class).getTabPersistenceHandler().setCurrentTab(configData.currentVerificationTab);
		
		groovyConsole.applySettings(configData.groovyConsoleSettings);
		bConsole.applySettings(configData.bConsoleSettings);

		this.uiState.setStatesViewColumnsWidth(configData.statesViewColumnsWidth);
		this.uiState.setStatesViewColumnsOrder(configData.statesViewColumnsOrder);

		this.uiState.setHorizontalDividerPositions(configData.horizontalDividerPositions);
		this.uiState.setVerticalDividerPositions(configData.verticalDividerPositions);

		this.uiState.setOperationsSortMode(configData.operationsSortMode);
		this.uiState.setOperationsShowNotEnabled(configData.operationsShowNotEnabled);
		
		this.globalPreferences.putAll(configData.globalPreferences);

		this.proBPluginManager.setPluginDirectory(configData.pluginDirectory);
	}

	public void save() {
		if (!this.runtimeOptions.isSaveConfig()) {
			logger.info("Config saving disabled via runtime options, ignoring config save request");
			return;
		}
		
		uiState.updateSavedStageBoxes();
		final ConfigData configData = new ConfigData();
		configData.localeOverride = this.uiState.getLocaleOverride();
		configData.guiState = this.uiState.getGuiState();
		configData.visibleStages = new ArrayList<>(this.uiState.getSavedVisibleStages());
		configData.stageBoxes = new HashMap<>();
		for (final Map.Entry<String, BoundingBox> entry : this.uiState.getSavedStageBoxes().entrySet()) {
			configData.stageBoxes.put(entry.getKey(), new double[] { entry.getValue().getMinX(),
					entry.getValue().getMinY(), entry.getValue().getWidth(), entry.getValue().getHeight(), });
		}
		configData.groovyObjectTabs = new ArrayList<>(this.uiState.getGroovyObjectTabs());
		configData.maxRecentProjects = this.recentProjects.getMaximum();
		configData.recentProjects = new ArrayList<>(this.recentProjects);
		configData.defaultProjectLocation = this.currentProject.getDefaultLocation().toString();
		configData.currentPreference = injector.getInstance(PreferencesStage.class).getTabPersistenceHandler().getCurrentTab();
		configData.currentMainTab = injector.getInstance(MainView.class).getTabPersistenceHandler().getCurrentTab();
		configData.currentVerificationTab = injector.getInstance(VerificationsView.class).getTabPersistenceHandler().getCurrentTab();
		configData.groovyConsoleSettings = groovyConsole.getSettings();
		configData.bConsoleSettings = bConsole.getSettings();
		configData.expandedTitledPanes = new ArrayList<>(this.uiState.getExpandedTitledPanes());

		TablePersistenceHandler tablePersistenceHandler = injector.getInstance(TablePersistenceHandler.class);

		StatesView statesView = injector.getInstance(StatesView.class);
		configData.statesViewColumnsWidth = tablePersistenceHandler.getColumnsWidth(statesView.getTable().getColumns());
		configData.statesViewColumnsOrder = tablePersistenceHandler.getColumnsOrder(statesView.getTable().getColumns());

		MainController main = injector.getInstance(MainController.class);

		configData.horizontalDividerPositions = main.getHorizontalDividerPositions();
		configData.verticalDividerPositions = main.getVerticalDividerPositions();

		OperationsView operationsView = injector.getInstance(OperationsView.class);
		configData.operationsSortMode = operationsView.getSortMode();
		configData.operationsShowNotEnabled = operationsView.getShowDisabledOps();
		
		configData.globalPreferences = new HashMap<>(this.globalPreferences);

		configData.pluginDirectory = proBPluginManager.getPluginDirectory().getAbsolutePath();

		try (
			final OutputStream os = new FileOutputStream(LOCATION);
			final Writer writer = new OutputStreamWriter(os, CONFIG_CHARSET)
		) {
			GSON.toJson(configData, writer);
		} catch (FileNotFoundException exc) {
			logger.warn("Failed to create config file", exc);
		} catch (IOException exc) {
			logger.warn("Failed to save config file", exc);
		}
	}
}
