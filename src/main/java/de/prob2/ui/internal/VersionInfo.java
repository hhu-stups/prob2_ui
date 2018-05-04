package de.prob2.ui.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob.cli.CliVersionNumber;
import de.prob.scripting.Api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class VersionInfo {
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionInfo.class);
	
	private final Injector injector;
	private final Properties buildInfo;
	private final Object lock;
	
	private CliVersionNumber cliVersion;
	
	@Inject
	private VersionInfo(final Injector injector) {
		super();
		
		this.injector = injector;
		
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
	
	public String getUIBuildTime() {
		return this.buildInfo.getProperty("buildTime");
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
				// Computed lazily, because Api.getVersion() starts a CLI.
				this.cliVersion = this.injector.getInstance(Api.class).getVersion();
			}
		}
		return this.cliVersion;
	}
	
	public String getFormattedCliVersion() {
		final CliVersionNumber cvn = this.getCliVersion();
		return String.format("%s.%s.%s-%s", cvn.major, cvn.minor, cvn.service, cvn.qualifier);
	}
}
