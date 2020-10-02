package de.prob2.ui.animation.tracereplay;

import java.nio.file.Path;
import java.util.Objects;

import com.google.inject.Injector;

import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ReplayTrace implements IExecutableItem, DescriptionView.Describable {
	private final ObjectProperty<Checked> status;
	private final DoubleProperty progress;
	private final Path location;
	private String errorMessageBundleKey;
	private BooleanProperty shouldExecute;
	private Object[] errorMessageParams;

	private final Injector injector;

	public ReplayTrace(Path location, Injector injector) {
		this.status = new SimpleObjectProperty<>(this, "status", Checked.NOT_CHECKED);
		this.progress = new SimpleDoubleProperty(this, "progress", -1);
		this.location = location;
		this.errorMessageBundleKey = null;
		this.shouldExecute = new SimpleBooleanProperty(true);
		this.injector = injector;
		
		this.status.addListener((o, from, to) -> {
			if (to != Checked.FAIL) {
				this.errorMessageBundleKey = null;
			}
		});
	}

	@Override
	public ObjectProperty<Checked> checkedProperty() {
		return status;
	}

	@Override
	public Checked getChecked() {
		return this.status.get();
	}
	
	public void setChecked(Checked status) {
		this.status.set(status);
	}
	
	public DoubleProperty progressProperty() {
		return this.progress;
	}
	
	public double getProgress() {
		return this.progressProperty().get();
	}
	
	public void setProgress(final double progress) {
		this.progressProperty().set(progress);
	}
	
	public Path getLocation() {
		return this.location;
	}
	
	public void setErrorMessageBundleKey(String errorMessageBundleKey) {
		this.errorMessageBundleKey = errorMessageBundleKey;
	}
	
	public String getErrorMessageBundleKey() {
		return errorMessageBundleKey;
	}
	
	public void setErrorMessageParams(Object... params) {
		this.errorMessageParams = params;
	}
	
	public Object[] getErrorMessageParams() {
		return errorMessageParams;
	}
	
	public void setSelected(boolean selected) {
		this.shouldExecute.set(selected);
	}
	
	@Override
	public boolean selected() {
		return shouldExecute.get();
	}
	
	public BooleanProperty selectedProperty() {
		return shouldExecute;
	}

	public String getName() {
		return location.getFileName().toString();
	}

	public String getDescription() {
		PersistentTrace trace = getPersistentTrace();
		if(trace != null) {
			return trace.getDescription();
		} else {
			return null;
		}

	}

	public void setDescription(String description) {
		TraceJsonFile trace = getTrace();
		if (trace != null) {
			trace.getTrace().setDescription(description);
			injector.getInstance(TraceFileHandler.class)
				.save(trace, injector.getInstance(CurrentProject.class).getLocation().resolve(location));
		}
	}

	PersistentTrace getPersistentTrace() {
		return injector.getInstance(TraceFileHandler.class).load_trace(this.getLocation());
	}

	TraceJsonFile getTrace() {
		return injector.getInstance(TraceFileHandler.class).load_complete(this.getLocation());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(location);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		if(!(obj instanceof ReplayTrace)) {
			return false;
		}
		return location.equals(((ReplayTrace) obj).getLocation());
	}
}
