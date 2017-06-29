package de.prob2.ui.config;

import com.google.common.base.MoreObjects;

public final class RuntimeOptions {
	private String project;
	private String runconfig;
	private boolean resetPreferences;
	
	public RuntimeOptions() {
		super();
		
		this.project = null;
		this.runconfig = null;
		this.resetPreferences = false;
	}
	
	public String getProject() {
		return this.project;
	}
	
	public void setProject(final String project) {
		this.project = project;
	}
	
	public String getRunconfig() {
		return this.runconfig;
	}
	
	public void setRunconfig(final String runconfig) {
		this.runconfig = runconfig;
	}
	
	public boolean isResetPreferences() {
		return this.resetPreferences;
	}
	
	public void setResetPreferences(final boolean resetPreferences) {
		this.resetPreferences = resetPreferences;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("project", this.project)
			.add("runconfig", this.runconfig)
			.add("resetPreferences", this.resetPreferences)
			.toString();
	}
}
