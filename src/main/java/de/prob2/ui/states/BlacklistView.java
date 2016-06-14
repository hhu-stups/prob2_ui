package de.prob2.ui.states;

import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;

import com.sun.javafx.collections.ObservableSetWrapper;
import de.prob.model.representation.AbstractElement;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.util.StringConverter;

public class BlacklistView implements Initializable {
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
			return clazz == null ? "" : clazz.getCanonicalName();
		}
	}
	
	@FXML ListView<Class<? extends AbstractElement>> list;
	ObservableSet<Class<? extends AbstractElement>> childrenClassBlacklist;
	
	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		this.childrenClassBlacklist = new ObservableSetWrapper<>(new HashSet<>());
		this.list.setCellFactory(CheckBoxListCell.forListView(clazz -> {
			BooleanProperty prop = new SimpleBooleanProperty(!this.childrenClassBlacklist.contains(clazz));
			
			prop.addListener(changed -> {
				if (((BooleanProperty) changed).get()) {
					this.childrenClassBlacklist.remove(clazz);
				} else {
					this.childrenClassBlacklist.add(clazz);
				}
			});
			
			this.childrenClassBlacklist.addListener((SetChangeListener.Change<? extends Class<? extends AbstractElement>> change) -> {
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
	}
}
