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
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.util.StringConverter;

public class BlacklistView implements Initializable {
	@SuppressWarnings("unchecked")
	private static class ElementClassStringConverter extends StringConverter<Class<? extends AbstractElement>> {
		public Class<? extends AbstractElement> fromString(String s) {
			Class<?> clazz;
			
			try {
				clazz = Class.forName(s);
			} catch (ClassNotFoundException e) {
				new Alert(Alert.AlertType.ERROR, "Could not find class named " + s).showAndWait();
				throw new IllegalArgumentException(e);
			}
			
			try {
				return clazz.asSubclass(AbstractElement.class);
			} catch (ClassCastException e) {
				new Alert(Alert.AlertType.ERROR, clazz + " is not a subclass of " + AbstractElement.class).showAndWait();
				throw new IllegalArgumentException(e);
			}
		}
		
		@Override
		public String toString(Class<? extends AbstractElement> clazz) {
			return clazz == null ? "" : clazz.getCanonicalName();
		}
	}
	
	@FXML ListView<Class<? extends AbstractElement>> list;
	/*
	private @FXML Button add;
	private @FXML Button remove;
	*/
	ObservableSet<Class<? extends AbstractElement>> childrenClassBlacklist;
	
	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		/*
		this.list.setCellFactory(TextFieldListCell.forListView(new ElementClassStringConverter()));
		
		this.add.setOnAction(event -> {
			this.list.getItems().add(Action.class);
			this.list.edit(this.list.getItems().size()-1);
		});
		this.remove.setOnAction(event -> {
			this.list.getItems().remove(this.list.getSelectionModel().getSelectedIndex());
		});
		
		this.list.setOnEditCommit(event -> {
			this.list.getItems().set(event.getIndex(), event.getNewValue());
		});
		*/
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
