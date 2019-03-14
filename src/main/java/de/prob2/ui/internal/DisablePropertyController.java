package de.prob2.ui.internal;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.animation.symbolic.SymbolicAnimationChecker;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.modelchecking.Modelchecker;
import de.prob2.ui.verifications.symbolicchecking.SymbolicFormulaChecker;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;

/*
* This class binds the disable property of a GUI element to the default disable property,
* when probcli is working.
*/

@Singleton
public class DisablePropertyController {

    private final BooleanExpression disableProperty;

    @Inject
    public DisablePropertyController(final Injector injector) {
        this.disableProperty = injector.getInstance(TraceChecker.class).currentJobThreadsProperty().emptyProperty().not()
                .or(injector.getInstance(Modelchecker.class).currentJobThreadsProperty().emptyProperty().not())
                .or(injector.getInstance(SymbolicAnimationChecker.class).currentJobThreadsProperty().emptyProperty().not())
                .or(injector.getInstance(SymbolicFormulaChecker.class).currentJobThreadsProperty().emptyProperty().not())
                .or(injector.getInstance(LTLFormulaChecker.class).currentJobThreadsProperty().emptyProperty().not());
    }

    public void addDisableProperty(final BooleanProperty guiDisableProperty) {
        guiDisableProperty.bind(disableProperty);
    }

    public void addDisableProperty(final BooleanProperty guiDisableProperty, BooleanExpression otherBindings) {
        guiDisableProperty.bind(otherBindings.or(disableProperty));
    }

}
