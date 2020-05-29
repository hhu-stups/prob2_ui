package de.prob2.ui.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.BParser;
import de.prob.Main;
import de.prob.animator.command.GetVersionCommand;
import de.prob.cli.CliVersionNumber;
import de.prob2.ui.project.MachineLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class VersionInfo {
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionInfo.class);
	
	private final MachineLoader machineLoader;
	private final Properties buildInfo;
	private final Object lock;
	
	private CliVersionNumber cliVersion;
	
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
	
	public String getUICommit() {
		return this.buildInfo.getProperty("commit");
	}
	
	public String getKernelVersion() {
		return Main.getVersion();
	}
	
	public String getKernelCommit() {
		return Main.getGitSha();
	}
	
	public CliVersionNumber getCliVersion() {
		synchronized (this.lock) {
			if (this.cliVersion == null) {
				// Computed lazily, because the empty state space potentially still needs to be started, which takes a few seconds.
				final GetVersionCommand cmd = new GetVersionCommand();
				this.machineLoader.getEmptyStateSpace().execute(cmd);
				this.cliVersion = cmd.getVersion();
			}
		}
		return this.cliVersion;
	}
	
	public String getFormattedCliVersion() {
		final CliVersionNumber cvn = this.getCliVersion();
		return String.format("%s.%s.%s-%s", cvn.major, cvn.minor, cvn.service, cvn.qualifier);
	}
	
	public String getParserVersion() {
		return BParser.getVersion();
	}
	
	public String getParserCommit() {
		return BParser.getGitSha();
	}
}
