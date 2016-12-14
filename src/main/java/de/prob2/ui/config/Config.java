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
import java.lang.ref.Reference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob.model.representation.AbstractElement;

import de.prob2.ui.consoles.b.BConsole;
import de.prob2.ui.consoles.groovy.GroovyConsole;
import de.prob2.ui.internal.UIState;
import de.prob2.ui.menu.RecentFiles;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.states.ClassBlacklist;

import javafx.geometry.BoundingBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
public final class Config {
	private static final class ConfigData {
		private int maxRecentFiles;
		private List<String> recentFiles;
		private String[] groovyConsoleSettings;
		private String[] bConsoleSettings;
		private List<String> statesViewHiddenClasses;
		private String guiState;
		private HashMap<String, double[]> stages;
		private List<String> groovyObjectTabs;
		private String currentPreference;
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
	
	@Inject
	private Config(final ClassBlacklist classBlacklist, 
					final RecentFiles recentFiles, 
					final UIState uiState, 
					final GroovyConsole groovyConsole, 
					final BConsole bConsole, 
					final Injector injector) {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.classBlacklist = classBlacklist;
		this.recentFiles = recentFiles;
		this.uiState = uiState;
		this.groovyConsole = groovyConsole;
		this.bConsole = bConsole;
		this.injector = injector;
		
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
			configData.recentFiles = new ArrayList<>(this.defaultData.recentFiles);
		}
		
		if (configData.statesViewHiddenClasses == null) {
			configData.statesViewHiddenClasses = new ArrayList<>(this.defaultData.statesViewHiddenClasses);
		}
		if (configData.guiState == null || configData.guiState.isEmpty()) {
			configData.guiState = this.defaultData.guiState;
		}
		if (configData.stages == null) {
			configData.stages = new HashMap<>(this.defaultData.stages);
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
		this.recentFiles.setAll(configData.recentFiles);
		
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
		
		for (final Map.Entry<String, double[]> entry : configData.stages.entrySet()) {
			final double[] v = entry.getValue();
			this.uiState.getSavedStageBoxes().put(entry.getKey(), new BoundingBox(v[0], v[1], v[2], v[3]));
		}
		
		for (String tab : configData.groovyObjectTabs) {
			this.uiState.addGroovyObjectTab(tab);
		}
		
		this.injector.getInstance(PreferencesStage.class).setCurrentTab(configData.currentPreference);
		
		groovyConsole.applySettings(configData.groovyConsoleSettings);
		bConsole.applySettings(configData.bConsoleSettings);
	}
		
	public void save() {
		final ConfigData configData = new ConfigData();
		configData.guiState = this.uiState.getGuiState();
		configData.stages = new HashMap<>();
		for (final Map.Entry<String, Reference<Stage>> entry : this.uiState.getStages().entrySet()) {
			final Stage stage = entry.getValue().get();
			if (stage == null) {
				continue;
			}
			configData.stages.put(entry.getKey(), new double[] {stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()});
		}
		configData.groovyObjectTabs = new ArrayList<>(this.uiState.getGroovyObjectTabs());
		configData.maxRecentFiles = this.recentFiles.getMaximum();
		configData.recentFiles = new ArrayList<>(this.recentFiles);
		configData.statesViewHiddenClasses = new ArrayList<>();
		configData.currentPreference = injector.getInstance(PreferencesStage.class).getCurrentTab();
		configData.groovyConsoleSettings = groovyConsole.getSettings();
		configData.bConsoleSettings = bConsole.getSettings();
		
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
