package de.prob2.ui.internal;

public abstract class AbstractResultHandler {
	protected final StageManager stageManager;
	protected final I18n i18n;
	

	
	protected AbstractResultHandler(final StageManager stageManager, final I18n i18n) {
		this.stageManager = stageManager;
		this.i18n = i18n;
	}
}
