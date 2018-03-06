package de.prob2.ui.plugin;


import org.pf4j.PluginWrapper;

/**
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 29.01.18
 */
public class InvalidPlugin extends ProBPlugin{

	private Exception exception;
	private String message;

	public InvalidPlugin(PluginWrapper wrapper) {
		super(wrapper, null, null);
	}

	public InvalidPlugin(PluginWrapper wrapper, String message) {
		this(wrapper);
		this.message = message;
	}

	public InvalidPlugin(PluginWrapper wrapper, String message, Exception exception) {
		this(wrapper, message);
		this.exception = exception;
	}

	@Override
	public String getName() {
		return "InvalidPlugin";
	}

	@Override
	public void startPlugin() {}

	@Override
	public void stopPlugin() {}

	public Exception getException() {
		return exception;
	}

	public String getMessage() {
		return message;
	}
}
