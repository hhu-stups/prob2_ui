package de.prob2.ui.internal;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

public final class DataPathsModule extends AbstractModule {
	private static final String APPDIRS_APP_NAME = "prob2-ui";
	private static final String APPDIRS_APP_AUTHOR = "STUPS";
	
	public DataPathsModule() {
		super();
	}
	
	@Provides
	@Singleton
	private static AppDirs getAppDirs() {
		return AppDirsFactory.getInstance();
	}
	
	@Provides
	@Singleton
	@ConfigFile
	private static Path getConfigFilePath(final AppDirs appDirs) {
		return Paths.get(appDirs.getUserConfigDir(APPDIRS_APP_NAME, null, APPDIRS_APP_AUTHOR), "config.json");
	}
	
	@Provides
	@Singleton
	@DefaultPluginDirectory
	private static Path getDefaultPluginsDirectoryPath(final AppDirs appDirs) {
		return Paths.get(appDirs.getUserDataDir(APPDIRS_APP_NAME, null, APPDIRS_APP_AUTHOR), "plugins");
	}
}
