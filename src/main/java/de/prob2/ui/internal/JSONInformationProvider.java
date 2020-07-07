package de.prob2.ui.internal;

import de.prob2.ui.prob2fx.CurrentProject;

public class JSONInformationProvider {

    public static String getKernelVersion(VersionInfo versionInfo) {
        return versionInfo.getKernelVersion();
    }

    public static String getCliVersion(VersionInfo versionInfo) {
        return versionInfo.getFormattedCliVersion();
    }

    public static String getModelName(CurrentProject currentProject) {
        if (currentProject.getCurrentMachine() == null) {
            throw new IllegalStateException("withCurrentModelName() can only be called while a machine is loaded");
        }
        return currentProject.getCurrentMachine().getName();
    }
}
