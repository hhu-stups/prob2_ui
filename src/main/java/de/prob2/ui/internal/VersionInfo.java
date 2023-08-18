package de.prob2.ui.internal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.BParser;
import de.prob.Main;
import de.prob.animator.command.GetVersionCommand;
import de.prob.cli.CliVersionNumber;
import de.prob2.ui.project.MachineLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Singleton
public final class VersionInfo {
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionInfo.class);
	
	private final MachineLoader machineLoader;
	private final Properties buildInfo;
	private final Object lock;
	
	private GetVersionCommand cliVersionCommand;
	
	@Inject
	private VersionInfo(final MachineLoader machineLoader) {
		super();
		
		this.machineLoader = machineLoader;
		
		this.buildInfo = new Properties();
		try (final Reader reader = new InputStreamReader(
			this.getClass().getResourceAsStream("/de/prob2/ui/build.properties"),
			StandardCharsets.UTF_8
		)) {
			this.buildInfo.load(reader);
		} catch (IOException e) {
			LOGGER.error("Failed to load build info", e);
		}
		
		this.lock = new Object();
	}
	
	public String getUIVersion() {
		return this.buildInfo.getProperty("version");
	}
	
	public String getUIBranch() {
		return this.buildInfo.getProperty("branch");
	}

	public String getUICommit() {
		return this.buildInfo.getProperty("commit");
	}
	
	public String getKernelVersion() {
		return Main.getVersion();
	}
	
	public String getKernelCommit() {
		return Main.getGitSha();
	}
	
	private GetVersionCommand loadCliVersionInfo() {
		if (this.cliVersionCommand == null) {
			synchronized (this.lock) {
				// Computed lazily to avoid excessive communication with the CLI.
				final GetVersionCommand cmd = new GetVersionCommand();
				this.machineLoader.getActiveStateSpace().execute(cmd);
				// Set the field only after the command has executed
				// to prevent storing and using a not fully initialized command object
				// if the command fails to execute.
				this.cliVersionCommand = cmd;
			}
		}
		return this.cliVersionCommand;
	}
	
	public CliVersionNumber getCliVersion() {
		return this.loadCliVersionInfo().getVersion();
	}
	
	public String getCliLastChangedDate() {
		return this.loadCliVersionInfo().getLastchangeddate();
	}
	
	public String getCliPrologInfo() {
		return this.loadCliVersionInfo().getProloginfo();
	}
	
	public String getParserVersion() {
		return BParser.getVersion();
	}
	
	public String getParserCommit() {
		return BParser.getGitSha();
	}
}
