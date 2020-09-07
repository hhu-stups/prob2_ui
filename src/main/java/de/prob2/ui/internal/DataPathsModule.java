package de.prob2.ui.internal;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import de.prob.Main;

public final class DataPathsModule extends AbstractModule {
	public DataPathsModule() {
		super();
	}
	
	@Provides
	@Singleton
	@ConfigFile
	private static Path getConfigFilePath() {
		return Paths.get(Main.getProBDirectory(), "prob2ui", "config.json");
	}
	
	@Provides
	@Singleton
	@DefaultPluginDirectory
	private static Path getDefaultPluginsDirectoryPath() {
		return Paths.get(Main.getProBDirectory(), "prob2ui", "plugins");
	}
}
