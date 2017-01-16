package de.prob2.ui.persistence;


import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.animations.Animation;
import de.prob2.ui.states.StateTreeItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Control;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

@Singleton
public class TablePersistenceHandler {
	
	private final UIState uiState;
	
	@Inject
	private TablePersistenceHandler(UIState uiState) {
		this.uiState = uiState;
	}

	public double[] getColumnsWidth(List<? extends TableColumnBase<?,?>> columns) {
		double[] result = new double[columns.size()];		
		for(int i = 0; i < columns.size(); i++) {
			result[i] = columns.get(i).getWidth();
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
		
		for (int i = 0; i < columns.size() - 1; i++) {
			if(tableEnum == TablePersistenceEnum.ANIMATIONS_VIEW) {
				((TableView<Animation>)control).resizeColumn((TableColumn<Animation,?>) columns.get(i), widths[i] - columns.get(i).getPrefWidth());
			} else {
				((TreeTableView<StateTreeItem<?>>)control).resizeColumn((TreeTableColumn<StateTreeItem<?>,?>) columns.get(i), widths[i] - columns.get(i).getPrefWidth());
			}
		}
	}
		
	@SuppressWarnings("unchecked")
	public void setColumnsOrder(ObservableList<? extends TableColumnBase<?,?>> columns,  TablePersistenceEnum tableEnum) {
		String[] order;
		if(tableEnum == TablePersistenceEnum.ANIMATIONS_VIEW) {
			order = uiState.getAnimationsViewColumnsOrder();
		} else {
			order = uiState.getStatesViewColumnsOrder();
		}
		
		
		ObservableList<? extends TableColumnBase<?,?>> newColumns = FXCollections.observableArrayList();
		for(int i = 0; i < order.length; i++) {
			for(TableColumnBase<?,?> column : columns) {
				if(((TableColumnBase<?,?>)column).getText().equals(order[i])) {
					((ObservableList<TableColumnBase<?,?>>)newColumns).add(column);
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
	
	@SuppressWarnings("rawtypes")
	public String[] getColumnsName(ObservableList<? extends TableColumnBase<?,?>> columns) {
		String[] result = new String[columns.size()];
		for(int i = 0; i < columns.size(); i++) {
			result[i] = ((SortedList<? extends TableColumnBase<?,?>>)columns.sorted()).get(0).getText();
		}
		return result;
	}
	
}
