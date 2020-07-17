package de.prob2.ui.internal;

import de.prob2.ui.project.machines.Machine;

public class JSONInformationProvider {

	private static String modelName;

	public static String getCliVersion(VersionInfo versionInfo) {
		return versionInfo.getCliVersion().getShortVersionString();
	}

	public static void loadModelName(Machine machine) {
		if(machine == null) {
			modelName = null;
			return;
		}
		modelName = machine.getName();
	}

	public static String getModelName() {
		return modelName;
	}
}
