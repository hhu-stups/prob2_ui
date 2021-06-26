package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class ReplayTrace implements IExecutableItem, DescriptionView.Describable {
	private final ObjectProperty<Checked> status;
	private final DoubleProperty progress;
	private final ListProperty<List<Checked>> postconditionStatus;
	private final Path location;
	private String errorMessageBundleKey;
	private BooleanProperty shouldExecute;
	private Object[] errorMessageParams;

	private final Injector injector;

	public ReplayTrace(Path location, Injector injector) {
		this.status = new SimpleObjectProperty<>(this, "status", Checked.NOT_CHECKED);
		this.progress = new SimpleDoubleProperty(this, "progress", -1);
		this.postconditionStatus = new SimpleListProperty<>(this, "postcondition", FXCollections.observableArrayList());
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

	public ListProperty<List<Checked>> postconditionStatusProperty() {
		return postconditionStatus;
	}

	public List<List<Checked>> getPostconditionStatus() {
		return postconditionStatus.get();
	}

	public void setPostconditionStatus(List<List<Checked>> postconditionStatus) {
		this.postconditionStatus.set(FXCollections.observableArrayList(postconditionStatus));
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
		TraceJsonFile file = getTraceJsonFile();
		if (file.getTransitionList() != null) {
			try {
				injector.getInstance(TraceFileHandler.class)
					.save(file.changeDescription(description),injector.getInstance(CurrentProject.class).getLocation().resolve(location));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public TraceJsonFile getTraceJsonFile() {
		return injector.getInstance(TraceFileHandler.class).loadFile(this.getLocation());
	}

	public PersistentTrace getPersistentTrace() {
		return injector.getInstance(TraceFileHandler.class).load(this.getLocation());
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
