package de.prob2.ui.internal;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import de.prob.Main;

public final class ConfigFilePathModule extends AbstractModule {
	public ConfigFilePathModule() {
		super();
	}
	
	@Provides
	@Singleton
	@ConfigFile
	private static Path getConfigFilePath() {
		return Paths.get(Main.getProBDirectory(), "prob2ui", "config.json");
	}
}
