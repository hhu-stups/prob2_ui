package de.prob2.ui.prob2fx;

import java.lang.ref.WeakReference;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.UIState;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.stage.Stage;

/**
 * <p>
 * A singleton read-only property holding the current foreground {@link Stage}.
 * If there is no JavaFX Stage in the foreground, the value is {@code null}.
 * </p>
 * <p>
 * This class keeps track of the current stage using listeners on
 * {@link Stage#focusedProperty()}. Because it is not possible to listen on this
 * property for every stage automatically, stages must be manually registered
 * using the {@link #register(Stage)} method. If a non-registered Stage is in
 * the foreground, the property value is {@code null}, as if the foreground
 * window was not a JavaFX Stage.
 * </p>
 */
@Singleton
public final class CurrentStage extends ReadOnlyObjectProperty<Stage> {
	private final ObjectProperty<Stage> stage;
	private UIState uiState;

	@Inject
	private CurrentStage(UIState uiState) {
		super();
		this.stage = new SimpleObjectProperty<>(this, "stage", null);
		this.uiState = uiState;
	}

	@Override
	public Object getBean() {
		return null;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public Stage get() {
		return this.stage.get();
	}

	@Override
	public void addListener(final ChangeListener<? super Stage> listener) {
		this.stage.addListener(listener);
	}

	@Override
	public void removeListener(final ChangeListener<? super Stage> listener) {
		this.stage.removeListener(listener);
	}

	@Override
	public void addListener(final InvalidationListener listener) {
		this.stage.addListener(listener);
	}

	@Override
	public void removeListener(final InvalidationListener listener) {
		this.stage.removeListener(listener);
	}

	public void register(final Stage stage, final String id) {
		stage.getProperties().put("id", id);
		stage.showingProperty().addListener((observable, from, to) -> {
			final String stageId = (String)stage.getProperties().get("id");
			if (to) {
				if (stageId != null) {
					uiState.getStages().put(stageId, new WeakReference<>(stage));
				}
			} else {
				// FIXME The main stage is special-cased at the moment.
				// The main way to exit the application is to close the main stage,
				// which would normally remove it from the stage map.
				// We don't want that to happen, otherwise the main stage's bounds
				// cannot be restored on the next launch.
				if (stageId != null && !"de.prob2.ui.ProB2".equals(stageId)) {
					uiState.getStages().remove(stageId);
				}
			}
		});
		stage.focusedProperty().addListener((observable, from, to) -> {
			if (to) {
				this.stage.set(stage);
			} else if (stage.equals(this.stage.get())) {
				this.stage.set(null);
			}
		});
	}
}
