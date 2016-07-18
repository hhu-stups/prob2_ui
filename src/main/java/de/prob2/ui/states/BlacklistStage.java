package de.prob2.ui.states;

import java.io.IOException;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.model.representation.AbstractElement;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;

@Singleton
public class BlacklistStage extends Stage {
	private static class ElementClassStringConverter extends StringConverter<Class<? extends AbstractElement>> {
		public Class<? extends AbstractElement> fromString(String s) {
			Class<?> clazz;
			
			try {
				clazz = Class.forName(s);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
			
			return clazz.asSubclass(AbstractElement.class);
		}
		
		@Override
		public String toString(Class<? extends AbstractElement> clazz) {
			return clazz == null ? "null" : clazz.getSimpleName();
		}
	}
	
	private ClassBlacklist classBlacklist;
	@FXML ListView<Class<? extends AbstractElement>> list;
	
	@Inject
	public BlacklistStage(final FXMLLoader loader, final ClassBlacklist classBlacklist) {
		this.classBlacklist = classBlacklist;
		
		loader.setLocation(this.getClass().getResource("blacklist_stage.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	public void initialize() {
		this.list.setCellFactory(CheckBoxListCell.forListView(clazz -> {
			BooleanProperty prop = new SimpleBooleanProperty(!this.classBlacklist.getBlacklist().contains(clazz));
			
			prop.addListener((changed, from, to) -> {
				if (to) {
					this.classBlacklist.getBlacklist().remove(clazz);
				} else {
					this.classBlacklist.getBlacklist().add(clazz);
				}
			});
			
			this.classBlacklist.getBlacklist().addListener((SetChangeListener<? super Class<? extends AbstractElement>>)change -> {
				if (clazz.equals(change.getElementAdded()) || clazz.equals(change.getElementRemoved())) {
					if (change.wasRemoved()) {
						prop.set(true);
					} else {
						prop.set(false);
					}
				}
			});
			
			return prop;
		}, new ElementClassStringConverter()));
		
		this.classBlacklist.getKnownClasses().addListener((SetChangeListener<? super Class<? extends AbstractElement>>)change -> {
			List<Class<? extends AbstractElement>> l = this.list.getItems();
			Class<? extends AbstractElement> added = change.getElementAdded();
			Class<? extends AbstractElement> removed = change.getElementRemoved();
			
			if (change.wasAdded() && !l.contains(added)) {
				l.add(added);
				l.sort((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()));
			} else if (change.wasRemoved() && l.contains(removed)) {
				if (this.classBlacklist.getBlacklist().contains(removed)) {
					this.classBlacklist.getBlacklist().remove(removed);
				}
				l.remove(removed);
			}
		});
	}
}
