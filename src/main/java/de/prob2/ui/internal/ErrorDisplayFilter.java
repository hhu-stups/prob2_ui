package de.prob2.ui.internal;

import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@Singleton
public final class ErrorDisplayFilter {
	/**
	 * Controls which level of errors/warnings is shown to the user.
	 * Errors/warnings are only displayed if their type is equal to or worse than the level set in this property.
	 * This should be set to at most {@link ErrorItem.Type#ERROR}.
	 * Setting it to {@link ErrorItem.Type#INTERNAL_ERROR} would hide details about all normal errors
	 * (but wouldn't suppress the corresponding exceptions).
	 */
	private final ObjectProperty<ErrorItem.Type> errorLevel;
	
	@Inject
	private ErrorDisplayFilter(final Config config) {
		super();
		
		this.errorLevel = new SimpleObjectProperty<>(this, "errorLevel", ErrorItem.Type.WARNING);
		
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.errorLevel != null) {
					ErrorDisplayFilter.this.setErrorLevel(configData.errorLevel);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.errorLevel = ErrorDisplayFilter.this.getErrorLevel();
			}
		});
	}
	
	public ObjectProperty<ErrorItem.Type> errorLevelProperty() {
		return this.errorLevel;
	}
	
	public ErrorItem.Type getErrorLevel() {
		return this.errorLevelProperty().get();
	}
	
	public void setErrorLevel(final ErrorItem.Type errorLevel) {
		this.errorLevelProperty().set(errorLevel);
	}
	
	/**
	 * Filter a list of errors based on the current error level.
	 * The returned list will contain only errors whose type is equal to or worse than the current error level.
	 * 
	 * @param errors the list of errors to filter
	 * @return the filtered list of errors (possibly empty)
	 */
	public List<ErrorItem> filterErrors(final List<ErrorItem> errors) {
		return errors.stream()
			.filter(error -> error.getType().compareTo(this.getErrorLevel()) >= 0)
			.collect(Collectors.toList());
	}
}
