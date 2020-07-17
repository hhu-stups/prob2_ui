package de.prob2.ui.internal;

public class JSONInformationProvider {
	public static String getCliVersion(VersionInfo versionInfo) {
		return versionInfo.getCliVersion().getShortVersionString();
	}
}
