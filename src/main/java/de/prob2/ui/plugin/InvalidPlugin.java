package de.prob2.ui.plugin;

public final class InvalidPlugin extends ProBPlugin {

	private final Exception exception;
	private final String messageBundleKey;
	private final String pluginClassName;

	InvalidPlugin(PluginContext context) {
		this(context, null, null);
	}

	InvalidPlugin(PluginContext context, String messageBundleKey, String pluginClassName) {
		this(context, messageBundleKey, pluginClassName, null);
	}

	InvalidPlugin(PluginContext context, String messageBundleKey, String pluginClassName, Exception exception) {
		super(context);
		this.exception = exception;
		this.messageBundleKey = messageBundleKey;
		this.pluginClassName = pluginClassName;
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

	@Override
	protected void deletePlugin() {
		throw new UnsupportedOperationException();
	}

	public Exception getException() {
		return this.exception;
	}

	public String getMessageBundleKey() {
		return this.messageBundleKey;
	}
	
	public String getPluginClassName() {
		return this.pluginClassName;
	}
}
