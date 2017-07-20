package de.prob2.ui.config;

import com.google.common.base.MoreObjects;

public final class RuntimeOptions {
	private final String project;
	private final String runconfig;
	private final boolean loadConfig;
	private final boolean saveConfig;
	
	public RuntimeOptions(final String project, final String runconfig, final boolean loadConfig, final boolean saveConfig) {
		super();
		
		this.project = project;
		this.runconfig = runconfig;
		this.loadConfig = loadConfig;
		this.saveConfig = saveConfig;
	}
	
	public RuntimeOptions() {
		this(null, null, true, true);
	}
	
	public String getProject() {
		return this.project;
	}
	
	public String getRunconfig() {
		return this.runconfig;
	}
	
	public boolean isLoadConfig() {
		return this.loadConfig;
	}
	
	public boolean isSaveConfig() {
		return this.saveConfig;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("project", this.project)
			.add("runconfig", this.runconfig)
			.add("loadConfig", this.loadConfig)
			.add("saveConfig", this.saveConfig)
			.toString();
	}
}
