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
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob.model.representation.AbstractElement;

import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.ConsoleInstructionOption;
import de.prob2.ui.consoles.b.BConsole;
import de.prob2.ui.consoles.groovy.GroovyConsole;
import de.prob2.ui.menu.RecentFiles;
import de.prob2.ui.states.ClassBlacklist;

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
	
	@Inject
	private Config(final ClassBlacklist classBlacklist, final RecentFiles recentFiles, final GroovyConsole groovyConsole, final BConsole bConsole) {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.classBlacklist = classBlacklist;
		this.recentFiles = recentFiles;
		this.groovyConsole = groovyConsole;
		this.bConsole = bConsole;
		
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
		
		if (configData.groovyConsoleEntries == null) {
			configData.groovyConsoleEntries = new ArrayList<>(this.defaultData.groovyConsoleEntries);
		}
		
		if (configData.bConsoleEntries == null) {
			configData.bConsoleEntries = new ArrayList<>(this.defaultData.bConsoleEntries);
		}
		
		if (configData.statesViewHiddenClasses == null) {
			configData.statesViewHiddenClasses = new ArrayList<>(this.defaultData.statesViewHiddenClasses);
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
		
		for(String instruction : configData.groovyConsoleEntries) {
			groovyConsole.getInstructions().add(new ConsoleInstruction(instruction, ConsoleInstructionOption.ENTER));
			groovyConsole.increaseCounter();
		}
		
		for(String instruction : configData.bConsoleEntries) {
			bConsole.getInstructions().add(new ConsoleInstruction(instruction, ConsoleInstructionOption.ENTER));
			bConsole.increaseCounter();
		}
	}
	
	public void save() {
		final ConfigData configData = new ConfigData();
		configData.maxRecentFiles = this.recentFiles.getMaximum();
		configData.recentFiles = new ArrayList<>(this.recentFiles);
		configData.groovyConsoleEntries = new ArrayList<>(groovyConsole.getInstructionEntries());
		configData.bConsoleEntries = new ArrayList<>(bConsole.getInstructionEntries());
		configData.statesViewHiddenClasses = new ArrayList<>();
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
