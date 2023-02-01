package de.prob2.ui.sharedviews;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.scene.Scene;
import javafx.stage.Stage;

@Singleton
public final class TraceSelectionStage extends Stage {
	@Inject
	private TraceSelectionStage(final StageManager stageManager, final I18n i18n, final TraceSelectionView traceSelectionView) {
		this.setTitle(i18n.translate("traceselection.stage.title"));
		this.setScene(new Scene(traceSelectionView));
		stageManager.register(this, this.getClass().getName());
	}
}
