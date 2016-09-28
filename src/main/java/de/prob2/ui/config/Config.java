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
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.Main;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class Config {
	private static final class ConfigData {
		private int maxRecentFiles;
		private List<String> recentFiles;
	}
	
	private static final File LOCATION = new File(Main.getProBDirectory() + File.separator + "prob2ui" + File.separator + "config.json");
	private static final Logger logger = LoggerFactory.getLogger(Config.class);
	
	private final Gson gson;
	
	private final IntegerProperty maxRecentFiles;
	private final ObservableList<String> recentFiles;
	
	@Inject
	private Config() {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		
		this.maxRecentFiles = new SimpleIntegerProperty();
		this.recentFiles = FXCollections.observableArrayList();
		
		if (!LOCATION.getParentFile().exists()) {
			if (!LOCATION.getParentFile().mkdirs()) {
				logger.warn("Failed to create the parent directory for the config file {}", LOCATION.getAbsolutePath());
			}
		}
		
		this.getRecentFiles().addListener((ListChangeListener<? super String>)change -> {
			if (change.getList().size() > this.getMaxRecentFiles()) {
				// Truncate the list of recent files if it is longer than the maximum
				change.getList().remove(this.getMaxRecentFiles(), change.getList().size());
			}
		});
		
		this.load();
	}
	
	public void load() {
		ConfigData configData;
		try (final Reader reader = new InputStreamReader(new FileInputStream(LOCATION))) {
			configData = gson.fromJson(reader, ConfigData.class);
		} catch (FileNotFoundException ignored) {
			// Config file doesn't exist yet, load and write the defaults
			configData = gson.fromJson(new InputStreamReader(this.getClass().getResourceAsStream("default.json")), ConfigData.class);
			this.save();
		} catch (IOException exc) {
			logger.warn("Failed to open config file", exc);
			return;
		}
		
		this.maxRecentFiles.set(configData.maxRecentFiles);
		this.recentFiles.setAll(configData.recentFiles);
	}
	
	public void save() {
		/*
		if (this.configData == null) {
			logger.warn("No config data loaded, cannot save config.");
			return;
		}
		*/
		
		final ConfigData configData = new ConfigData();
		configData.maxRecentFiles = this.getMaxRecentFiles();
		configData.recentFiles = new ArrayList<>(this.getRecentFiles());
		
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(LOCATION))) {
			gson.toJson(configData, writer);
		} catch (FileNotFoundException exc) {
			logger.warn("Failed to create config file", exc);
		} catch (IOException exc) {
			logger.warn("Failed to save config file", exc);
		}
	}
	
	public IntegerProperty maxRecentFilesProperty() {
		return this.maxRecentFiles;
	}
	
	public int getMaxRecentFiles() {
		return this.maxRecentFilesProperty().get();
	}
	
	public void setMaxRecentFiles(final int maxRecentFiles) {
		this.maxRecentFilesProperty().set(maxRecentFiles);
	}
	
	public ObservableList<String> getRecentFiles() {
		return this.recentFiles;
	}
}
