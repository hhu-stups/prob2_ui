package de.prob2.ui.verifications;

import com.google.inject.Injector;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.modelchecking.ModelCheckStats;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaHandler;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class ShouldExecuteValueFactory implements Callback<TableColumn.CellDataFeatures<IShouldExecuteItem, CheckBox>, ObservableValue<CheckBox>> {
    
	public enum Type {
		LTL,SYMBOLIC,MODELCHECKING
	}
	
	private Type type;
	
	private final Injector injector;
	
	public ShouldExecuteValueFactory(Type type, final Injector injector) {
		this.injector = injector;
		
		this.type = type;
	}
	
	@Override
    public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<IShouldExecuteItem, CheckBox> param) {
    	IShouldExecuteItem item = param.getValue();
        CheckBox checkBox = new CheckBox();
        checkBox.selectedProperty().setValue(item.shouldExecute());
        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            item.setShouldExecute(newValue);
            Machine machine;
            switch(type) {
            	case MODELCHECKING:
            		injector.getInstance(ModelCheckStats.class).updateCurrentMachineStatus();
            		break;
            	case LTL:
            		machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
            		injector.getInstance(LTLFormulaChecker.class).checkMachineStatus(machine);
            		break;
            	case SYMBOLIC:
            		machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
            		injector.getInstance(SymbolicCheckingFormulaHandler.class).updateMachineStatus(machine);
            		break;
            	default:
            		break;
            }
        });
        return new SimpleObjectProperty<>(checkBox);
    }
}