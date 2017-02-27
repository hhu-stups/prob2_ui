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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob.model.representation.AbstractElement;
import de.prob2.ui.MainController;
import de.prob2.ui.consoles.Console;
import de.prob2.ui.consoles.b.BConsole;
import de.prob2.ui.consoles.groovy.GroovyConsole;
import de.prob2.ui.menu.RecentFiles;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.persistence.TablePersistenceHandler;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.states.ClassBlacklist;
import de.prob2.ui.states.StatesView;
import javafx.geometry.BoundingBox;

@Singleton
@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
public final class Config {
	private static final class ConfigData {
		private int maxRecentFiles;
		private List<String> recentFiles;
		private List<String> recentProjects;
		private Console.ConfigData groovyConsoleSettings;
		private Console.ConfigData bConsoleSettings;
		private List<String> statesViewHiddenClasses;
		private String guiState;
		private List<String> visibleStages;
		private Map<String, double[]> stageBoxes;
		private List<String> groovyObjectTabs;
		private String currentPreference;
		private List<String> expandedTitledPanes;
		private String defaultProjectLocation;
		private double[] horizontalDividerPositions;
		private double[] verticalDividerPositions;
		private double[] statesViewColumnsWidth;
		private String[] statesViewColumnsOrder;
		private double[] animationsViewColumnsWidth;
		private String[] animationsViewColumnsOrder;
		private OperationsView.SortMode operationsSortMode;
		private boolean operationsShowNotEnabled;
		private ConfigData() {}
	}
	
	private static final Charset CONFIG_CHARSET = Charset.forName("UTF-8");
	private static final File LOCATION = new File(Main.getProBDirectory() + File.separator + "prob2ui" + File.separator + "config.json");
	
	private static final Logger logger = LoggerFactory.getLogger(Config.class);
	
	private final Gson gson;
	private final ClassBlacklist classBlacklist;
	private final RecentFiles recentFiles;
	private final ConfigData defaultData;
	private final GroovyConsole groovyConsole;
	private final BConsole bConsole;
	private final Injector injector;
	private final UIState uiState;
	private final CurrentProject currentProject;
	
	@Inject
	private Config(final ClassBlacklist classBlacklist, 
					final RecentFiles recentFiles, 
					final UIState uiState, 
					final GroovyConsole groovyConsole, 
					final BConsole bConsole, 
					final Injector injector,
					final CurrentProject currentProject) {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.classBlacklist = classBlacklist;
		this.recentFiles = recentFiles;
		this.uiState = uiState;
		this.groovyConsole = groovyConsole;
		this.bConsole = bConsole;
		this.injector = injector;
		this.currentProject = currentProject;

		try (
			final InputStream is = Config.class.getResourceAsStream("default.json");
			final Reader defaultReader = new InputStreamReader(is, CONFIG_CHARSET)
		) {
			this.defaultData = gson.fromJson(defaultReader, ConfigData.class);
		} catch (FileNotFoundException exc) {
			throw new IllegalStateException("Default config file not found", exc);
		} catch (IOException exc) {
			throw new IllegalStateException("Failed to open default config file", exc);
		}
		
		if (!LOCATION.getParentFile().exists() && !LOCATION.getParentFile().mkdirs()) {
			logger.warn("Failed to create the parent directory for the config file {}", LOCATION.getAbsolutePath());
		}
		
		this.load();
	}
	
	private void replaceMissingWithDefaults(final ConfigData configData) {
		// If some keys are null (for example when loading a config from a previous version that did not have those keys), replace them with their values from the default config.
		if (configData.recentFiles == null) {
			configData.maxRecentFiles = this.defaultData.maxRecentFiles;
			configData.recentFiles = this.defaultData.recentFiles;
		}
		if (configData.recentProjects == null) {
			configData.recentProjects = this.defaultData.recentProjects;
		}
		if (configData.statesViewHiddenClasses == null) {
			configData.statesViewHiddenClasses = new ArrayList<>(this.defaultData.statesViewHiddenClasses);
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
		if(configData.groovyConsoleSettings == null) {
			configData.groovyConsoleSettings = this.defaultData.groovyConsoleSettings;
		}
		if(configData.bConsoleSettings == null) {
			configData.bConsoleSettings = this.defaultData.bConsoleSettings;
		}
		if(configData.expandedTitledPanes == null) {
			configData.expandedTitledPanes = this.defaultData.expandedTitledPanes;
		}
		if(configData.defaultProjectLocation == null) {
			configData.defaultProjectLocation = System.getProperty("user.home");
		}
		
		MainController main = injector.getInstance(MainController.class);
		
		if(configData.horizontalDividerPositions == null) {
			configData.horizontalDividerPositions = main.getHorizontalDividerPositions();
		}
		
		if(configData.verticalDividerPositions == null) {
			configData.verticalDividerPositions = main.getVerticalDividerPositions();
		}
				
		if(configData.statesViewColumnsWidth == null) {
			configData.statesViewColumnsWidth = this.defaultData.statesViewColumnsWidth.clone();
		}
		
		if(configData.statesViewColumnsOrder == null) {
			configData.statesViewColumnsOrder = this.defaultData.statesViewColumnsOrder.clone();
		}
		
		if(configData.animationsViewColumnsWidth == null) {
			configData.animationsViewColumnsWidth = this.defaultData.animationsViewColumnsWidth.clone();
		}
		
		if(configData.animationsViewColumnsOrder == null) {
			configData.animationsViewColumnsOrder = this.defaultData.animationsViewColumnsOrder.clone();
		}
		
		if(configData.operationsSortMode == null) {
			configData.operationsSortMode = this.defaultData.operationsSortMode;
		}
		
	}
	
	public void load() {
		ConfigData configData;
		try (
			final InputStream is = new FileInputStream(LOCATION);
			final Reader reader = new InputStreamReader(is, CONFIG_CHARSET)
		) {
			configData = gson.fromJson(reader, ConfigData.class);
		} catch (FileNotFoundException exc) {
			logger.info("Config file not found, loading default settings", exc);
			configData = this.defaultData;
		} catch (IOException exc) {
			logger.warn("Failed to open config file", exc);
			return;
		}
		
		this.replaceMissingWithDefaults(configData);
		
		this.recentFiles.setMaximum(configData.maxRecentFiles);
		this.recentFiles.setRecentFiles(configData.recentFiles);
		this.recentFiles.setRecentProjects(configData.recentProjects);
		
		this.currentProject.setDefaultLocation(Paths.get(configData.defaultProjectLocation));
		
		for (String name : configData.statesViewHiddenClasses) {
			Class<? extends AbstractElement> clazz;
			try {
				clazz = Class.forName(name).asSubclass(AbstractElement.class);
			} catch (ClassNotFoundException exc) {
				logger.warn("Class not found, cannot add to states view blacklist", exc);
				continue;
			} catch (ClassCastException exc) {
				logger.warn("Class is not a subclass of AbstractElement, cannot add to states view blacklist", exc);
				continue;
			}
			classBlacklist.getKnownClasses().add(clazz);
			classBlacklist.getBlacklist().add(clazz);
		}
		
		this.uiState.setGuiState(configData.guiState);
		this.uiState.getSavedVisibleStages().clear();
		this.uiState.getSavedVisibleStages().addAll(configData.visibleStages);
		
		for (final Map.Entry<String, double[]> entry : configData.stageBoxes.entrySet()) {
			final double[] v = entry.getValue();
			this.uiState.getSavedStageBoxes().put(entry.getKey(), new BoundingBox(v[0], v[1], v[2], v[3]));
		}

		configData.groovyObjectTabs.forEach(this.uiState::addGroovyObjectTab);
		
		for (String pane: configData.expandedTitledPanes) {
			this.uiState.getExpandedTitledPanes().add(pane);
		}
		
		this.injector.getInstance(PreferencesStage.class).setCurrentTab(configData.currentPreference);
		
		groovyConsole.applySettings(configData.groovyConsoleSettings);
		bConsole.applySettings(configData.bConsoleSettings);
		
		this.uiState.setStatesViewColumnsWidth(configData.statesViewColumnsWidth);
		this.uiState.setStatesViewColumnsOrder(configData.statesViewColumnsOrder);
		
		this.uiState.setAnimationsViewColumnsWidth(configData.animationsViewColumnsWidth);
		this.uiState.setAnimationsViewColumnsOrder(configData.animationsViewColumnsOrder);
		
		this.uiState.setHorizontalDividerPositions(configData.horizontalDividerPositions);
		this.uiState.setVerticalDividerPositions(configData.verticalDividerPositions);
		
		this.uiState.setOperationsSortMode(configData.operationsSortMode);
		this.uiState.setOperationsShowNotEnabled(configData.operationsShowNotEnabled);
	}
		
	public void save() {
		uiState.updateSavedStageBoxes();
		final ConfigData configData = new ConfigData();
		configData.guiState = this.uiState.getGuiState();
		configData.visibleStages = new ArrayList<>(this.uiState.getSavedVisibleStages());
		configData.stageBoxes = new HashMap<>();
		for (final Map.Entry<String, BoundingBox> entry : this.uiState.getSavedStageBoxes().entrySet()) {
			configData.stageBoxes.put(entry.getKey(), new double[] {
				entry.getValue().getMinX(),
				entry.getValue().getMinY(),
				entry.getValue().getWidth(),
				entry.getValue().getHeight(),
			});
		}
		configData.groovyObjectTabs = new ArrayList<>(this.uiState.getGroovyObjectTabs());
		configData.maxRecentFiles = this.recentFiles.getMaximum();
		configData.recentFiles = this.recentFiles.getRecentFiles();
		configData.recentProjects = this.recentFiles.getRecentProjects();
		configData.defaultProjectLocation = this.currentProject.getDefaultLocation().toString();
		configData.statesViewHiddenClasses = new ArrayList<>();
		configData.currentPreference = injector.getInstance(PreferencesStage.class).getCurrentTab();
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

		for (Class<? extends AbstractElement> clazz : classBlacklist.getBlacklist()) {
			configData.statesViewHiddenClasses.add(clazz.getCanonicalName());
		}
		
		try (
			final OutputStream os = new FileOutputStream(LOCATION);
			final Writer writer = new OutputStreamWriter(os, CONFIG_CHARSET)
		) {
			gson.toJson(configData, writer);
		} catch (FileNotFoundException exc) {
			logger.warn("Failed to create config file", exc);
		} catch (IOException exc) {
			logger.warn("Failed to save config file", exc);
		}
	}
}
