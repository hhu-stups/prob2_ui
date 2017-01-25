package de.prob2.ui.persistence;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.animations.Animation;
import de.prob2.ui.states.StateTreeItem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class TablePersistenceHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TablePersistenceHandler.class);
	
	private final UIState uiState;
	
	@Inject
	private TablePersistenceHandler(UIState uiState) {
		this.uiState = uiState;
	}

	public double[] getColumnsWidth(List<? extends TableColumnBase<?,?>> columns) {
		double[] result = new double[columns.size()];
		double sum = 0.0;
		for (int i = 0; i < columns.size(); i++) {
			result[i] = columns.get(i).getWidth();
			sum += result[i];
		}
		for (int i = 0; i < result.length; i++) {
			result[i] /= sum;
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public void setColumnsWidth(Control control, ObservableList<? extends TableColumnBase<?,?>> columns, TablePersistenceEnum tableEnum) {
		if(!(control instanceof TreeTableView || control instanceof TableView)) {
			return;
		}
		
		double[] widths;
		if(tableEnum == TablePersistenceEnum.ANIMATIONS_VIEW) {
			widths = uiState.getAnimationsViewColumnsWidth();
		} else {
			widths = uiState.getStatesViewColumnsWidth();
		}
		
		double sum = 0.0;
		for (final TableColumnBase<?, ?> column : columns) {
			sum += column.getWidth();
		}
		
		for (int i = 0; i < columns.size() - 1; i++) {
			if (tableEnum == TablePersistenceEnum.ANIMATIONS_VIEW) {
				((TableView<Animation>)control).resizeColumn((TableColumn<Animation,?>) columns.get(i), widths[i]*sum - columns.get(i).getWidth());
			} else {
				((TreeTableView<StateTreeItem<?>>)control).resizeColumn((TreeTableColumn<StateTreeItem<?>, ?>) columns.get(i), widths[i]*sum - columns.get(i).getWidth());
			}
		}
	}
		
	@SuppressWarnings("unchecked")
	public void setColumnsOrder(ObservableList<? extends TableColumnBase<?,?>> columns, TablePersistenceEnum tableEnum) {
		String[] order;
		if(tableEnum == TablePersistenceEnum.ANIMATIONS_VIEW) {
			order = uiState.getAnimationsViewColumnsOrder();
		} else {
			order = uiState.getStatesViewColumnsOrder();
		}
		
		ObservableList<? extends TableColumnBase<?,?>> newColumns = FXCollections.observableArrayList();
		for (final String text : order) {
			for (TableColumnBase<?, ?> column : columns) {
				if (column.getText().equals(text)) {
					((ObservableList<TableColumnBase<?, ?>>)newColumns).add(column);
				}
			}
		}	
		
		columns.clear();
		((ObservableList<TableColumnBase<?,?>>)columns).addAll(newColumns);
	}
	
	
	public String[] getColumnsOrder(List<? extends TableColumnBase<?,?>> columns) {
		String[] order = new String[columns.size()];
		for(int i = 0; i < columns.size(); i++) {
			order[i] = columns.get(i).getText();
		}
		return order;
	}
}
