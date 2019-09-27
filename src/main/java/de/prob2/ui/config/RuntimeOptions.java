package de.prob2.ui.config;

import com.google.common.base.MoreObjects;

public final class RuntimeOptions {
	private final String machineFile;
	private final String project;
	private final String machine;
	private final String preference;
	private final boolean loadConfig;
	private final boolean saveConfig;
	
	public RuntimeOptions(
		final String machineFile,
		final String project,
		final String machine,
		final String preference,
		final boolean loadConfig,
		final boolean saveConfig
	) {
		super();
		
		this.machineFile = machineFile;
		this.project = project;
		this.machine = machine;
		this.preference = preference;
		this.loadConfig = loadConfig;
		this.saveConfig = saveConfig;
	}
	
	public RuntimeOptions() {
		this(null, null, null, null, true, true);
	}
	
	public String getMachineFile() {
		return this.machineFile;
	}
	
	public String getProject() {
		return this.project;
	}
	
	public String getMachine() {
		return this.machine;
	}
	
	public String getPreference() {
		return this.preference;
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
			.add("machineFile", this.machineFile)
			.add("project", this.project)
			.add("machine", this.machine)
			.add("preference", this.preference)
			.add("loadConfig", this.loadConfig)
			.add("saveConfig", this.saveConfig)
			.toString();
	}
}
