package de.prob2.ui.prob2fx;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.stage.Stage;

@Singleton
public final class CurrentStage extends ReadOnlyObjectProperty<Stage> {
	private final ObjectProperty<Stage> stage;
	
	@Inject
	private CurrentStage() {
		super();
		
		this.stage = new SimpleObjectProperty<>(null);
	}
	
	@Override
	public Object getBean() {
		return this.stage.getBean();
	}
	
	@Override
	public String getName() {
		return this.stage.getName();
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
	
	private void set(final Stage stage) {
		this.stage.set(stage);
	}
	
	public void register(final Stage stage) {
		stage.focusedProperty().addListener((observable, from, to) -> {
			if (to) {
				this.stage.set(stage);
			} else if (stage.equals(this.stage.get())) {
				this.stage.set(null);
			}
		});
	}
}
