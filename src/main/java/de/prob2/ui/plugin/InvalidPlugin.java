package de.prob2.ui.plugin;

import org.pf4j.PluginWrapper;

public class InvalidPlugin extends ProBPlugin{

	private Exception exception;
	private String messageBundleKey;
	private String pluginClassName;

	public InvalidPlugin(PluginWrapper wrapper) {
		super(wrapper, null, null);
	}

	public InvalidPlugin(PluginWrapper wrapper, String messageBundleKey, String pluginClassName) {
		this(wrapper);
		this.messageBundleKey = messageBundleKey;
		this.pluginClassName = pluginClassName;
	}

	public InvalidPlugin(PluginWrapper wrapper, String messageBundleKey, String pluginClassName, Exception exception) {
		this(wrapper, messageBundleKey, pluginClassName);
		this.exception = exception;
	}

	@Override
	public String getName() {
		return "InvalidPlugin";
	}

	@Override
	public void startPlugin() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void stopPlugin() {
		throw new UnsupportedOperationException();
	}

	public Exception getException() {
		return exception;
	}

	public String getMessageBundleKey() {
		return messageBundleKey;
	}
	
	public String getPluginClassName() {
		return pluginClassName;
	}
}
