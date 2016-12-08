package de.prob2.ui.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob.model.representation.AbstractElement;

import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.ConsoleInstructionOption;
import de.prob2.ui.consoles.b.BConsole;
import de.prob2.ui.consoles.groovy.GroovyConsole;
import de.prob2.ui.internal.UIState;
import de.prob2.ui.menu.RecentFiles;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.states.ClassBlacklist;
import javafx.scene.control.IndexRange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
public final class Config {
	private static final class ConfigData {
		private int maxRecentFiles;
		private List<String> recentFiles;
		private List<String> groovyConsoleEntries;
		private List<String> bConsoleEntries;
		private List<String> statesViewHiddenClasses;
		private String guiState;
		private List<String> stages;
		private String groovyConsoleState;
		private int groovyConsoleCharCounterInLine;
		private int groovyConsoleCurrentPosInLine;
		private int groovyConsoleCaret;
		private List<IndexRange> groovyConsoleErrors;
		private String bConsoleState;
		private int bConsoleCharCounterInLine;
		private int bConsoleCurrentPosInLine;
		private int bConsoleCaret;
		private List<IndexRange> bConsoleErrors;
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
		
		try (final Reader defaultReader = new InputStreamReader(Config.class.getResourceAsStream("default.json"), CONFIG_CHARSET)) {
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
		if(configData.guiState == null || "".equals(configData.guiState)) {
			configData.guiState = this.defaultData.guiState;
		}
		if(configData.stages == null) {
			configData.stages = new ArrayList<>(this.defaultData.stages);
		}
		if(configData.currentPreference == null) {
			configData.currentPreference = this.defaultData.currentPreference;
		}
		this.replaceMissingWithDefaultsConsoles(configData);

	}
	
	private void replaceMissingWithDefaultsConsoles(final ConfigData configData) {
		if (configData.groovyConsoleEntries == null) {
			configData.groovyConsoleEntries = new ArrayList<>(this.defaultData.groovyConsoleEntries);
		}
		
		if (configData.bConsoleEntries == null) {
			configData.bConsoleEntries = new ArrayList<>(this.defaultData.bConsoleEntries);
		}
		if(configData.groovyConsoleState == null) {
			configData.groovyConsoleState = this.defaultData.groovyConsoleState;
			configData.groovyConsoleCharCounterInLine = this.defaultData.groovyConsoleCharCounterInLine;
			configData.groovyConsoleCurrentPosInLine = this.defaultData.groovyConsoleCurrentPosInLine;
			configData.groovyConsoleCaret = this.defaultData.groovyConsoleCaret;
			configData.groovyConsoleErrors = new ArrayList<>(this.defaultData.groovyConsoleErrors);
		}
		if(configData.bConsoleState == null) {
			configData.bConsoleState = this.defaultData.bConsoleState;
			configData.bConsoleCharCounterInLine = this.defaultData.bConsoleCharCounterInLine;
			configData.bConsoleCurrentPosInLine = this.defaultData.bConsoleCurrentPosInLine;
			configData.bConsoleCaret = this.defaultData.bConsoleCaret;
			configData.bConsoleErrors = new ArrayList<>(this.defaultData.bConsoleErrors);
		}
	}
	
	private void loadConsoles(final ConfigData configData) {
		for(String instruction : configData.groovyConsoleEntries) {
			groovyConsole.getInstructions().add(new ConsoleInstruction(instruction, ConsoleInstructionOption.ENTER));
			groovyConsole.increaseCounter();
		}
		
		for(String instruction : configData.bConsoleEntries) {
			bConsole.getInstructions().add(new ConsoleInstruction(instruction, ConsoleInstructionOption.ENTER));
			bConsole.increaseCounter();
		}
		
		this.groovyConsole.replaceText(configData.groovyConsoleState);
		this.groovyConsole.setCharCounterInLine(configData.groovyConsoleCharCounterInLine);
		this.groovyConsole.setCurrentPosInLine(configData.groovyConsoleCurrentPosInLine);
		this.groovyConsole.moveTo(configData.groovyConsoleCaret);
		
		for(IndexRange range: configData.groovyConsoleErrors) {
			groovyConsole.getErrors().add(range);
			groovyConsole.setStyleClass(range.getStart(), range.getEnd(), "error");
		}
		
		this.bConsole.replaceText(configData.bConsoleState);
		this.bConsole.setCharCounterInLine(configData.bConsoleCharCounterInLine);
		this.bConsole.setCurrentPosInLine(configData.bConsoleCurrentPosInLine);
		this.bConsole.moveTo(configData.bConsoleCaret);
		
		for(IndexRange range: configData.bConsoleErrors) {
			bConsole.getErrors().add(range);
			bConsole.setStyleClass(range.getStart(), range.getEnd(), "error");
		}
	}
	
	public void load() {
		ConfigData configData;
		try (final Reader reader = new InputStreamReader(new FileInputStream(LOCATION), CONFIG_CHARSET)) {
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
		
		for(String stage: configData.stages) {
			this.uiState.addStage(stage);
		}
		
		this.injector.getInstance(PreferencesStage.class).setCurrentTab(configData.currentPreference);
		this.loadConsoles(configData);
	}
	
	private void saveConsoles(final ConfigData configData) {
		configData.groovyConsoleEntries = new ArrayList<>(groovyConsole.getInstructionEntries());
		configData.bConsoleEntries = new ArrayList<>(bConsole.getInstructionEntries());
		configData.groovyConsoleState = groovyConsole.getText();
		configData.groovyConsoleCharCounterInLine = groovyConsole.getCharCounterInLine();
		configData.groovyConsoleCurrentPosInLine = groovyConsole.getCurrentPosInLine();
		configData.groovyConsoleCaret = groovyConsole.getCaretPosition();
		configData.groovyConsoleErrors = new ArrayList<>(groovyConsole.getErrors());
		configData.bConsoleState = bConsole.getText();
		configData.bConsoleCharCounterInLine = bConsole.getCharCounterInLine();
		configData.bConsoleCurrentPosInLine = bConsole.getCurrentPosInLine();
		configData.bConsoleCaret = bConsole.getCaretPosition();
		configData.bConsoleErrors = new ArrayList<>(bConsole.getErrors());
	}
	
	
	public void save() {
		final ConfigData configData = new ConfigData();
		configData.guiState = this.uiState.getGuiState();
		configData.stages = new ArrayList<>(this.uiState.getStages());
		configData.maxRecentFiles = this.recentFiles.getMaximum();
		configData.recentFiles = new ArrayList<>(this.recentFiles);
		configData.statesViewHiddenClasses = new ArrayList<>();
		configData.currentPreference = injector.getInstance(PreferencesStage.class).getCurrentTab();
		this.saveConsoles(configData);

		for (Class<? extends AbstractElement> clazz : classBlacklist.getBlacklist()) {
			configData.statesViewHiddenClasses.add(clazz.getCanonicalName());
		}
		
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(LOCATION), CONFIG_CHARSET)) {
			gson.toJson(configData, writer);
		} catch (FileNotFoundException exc) {
			logger.warn("Failed to create config file", exc);
		} catch (IOException exc) {
			logger.warn("Failed to save config file", exc);
		}
	}
	
}
