package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SetProperty;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class VOManager {

    private final CurrentProject currentProject;

    private final Injector injector;

    private final VOChecker voChecker;

    @Inject
    public VOManager(final CurrentProject currentProject, final Injector injector, final VOChecker voChecker) {
        this.currentProject = currentProject;
        this.injector = injector;
        this.voChecker = voChecker;
    }


    public void synchronizeMachine(Machine machine) {
        for(Requirement requirement : machine.getRequirements()) {
            for(ValidationObligation validationObligation : requirement.validationObligationsProperty()) {
                IExecutableItem executable = lookupExecutable(machine, validationObligation.getTask(), validationObligation.getItem());
                validationObligation.setExecutable(executable);
                validationObligation.checkedProperty().addListener((observable, from, to) -> requirement.updateChecked());
            }
            requirement.updateChecked();
        }
    }

    private IExecutableItem lookupExecutable(Machine machine, ValidationTask task, Object executableItem) {
        switch (task) {
            case MODEL_CHECKING:
                return machine.getModelcheckingItems().stream()
                        .filter(item -> item.getOptions().equals(((ModelCheckingItem) executableItem).getOptions()))
                        .findAny()
                        .orElse(null);
            case LTL_MODEL_CHECKING:
                return machine.getLTLFormulas().stream()
                        .filter(item -> item.settingsEqual((LTLFormulaItem) executableItem))
                        .findAny()
                        .orElse(null);
            case SYMBOLIC_MODEL_CHECKING:
                // TODO: Implement
                break;
            case TRACE_REPLAY:
                return injector.getInstance(TraceViewHandler.class).getTraces().stream()
                        .filter(item -> item.getLocation().toString().equals(executableItem))
                        .findAny()
                        .orElse(null);
            default:
                throw new RuntimeException("Validation task is not valid: " + task);
        }
        return null;
    }

    private ValidationObligation linkValidationObligation(Object item) {
        ValidationObligation validationObligation;
        if(item instanceof ModelCheckingItem) {
            validationObligation = new ValidationObligation(ValidationTask.MODEL_CHECKING, ((ModelCheckingItem) item).getOptions().toString(), item);
        } else if(item instanceof LTLFormulaItem) {
            validationObligation = new ValidationObligation(ValidationTask.LTL_MODEL_CHECKING, ((LTLFormulaItem) item).getCode(), item);
        } else if(item instanceof SymbolicCheckingFormulaItem) {
            // TODO: Implement
            return null;
        } else if(item instanceof ReplayTrace) {
            validationObligation = new ValidationObligation(ValidationTask.TRACE_REPLAY, ((ReplayTrace) item).getName(), ((ReplayTrace) item).getLocation().toString());
        } else {
            throw new RuntimeException("Validation item is not valid. Class is: " + item.getClass());
        }
        updateExecutableInVO(validationObligation);
        return validationObligation;
    }

    private void updateExecutableInVO(ValidationObligation validationObligation) {
        switch (validationObligation.getTask()) {
            case MODEL_CHECKING:
            case LTL_MODEL_CHECKING:
                validationObligation.setExecutable((IExecutableItem) validationObligation.getItem());
                break;
            case SYMBOLIC_MODEL_CHECKING:
                // TODO: Implement
                break;
            case TRACE_REPLAY:
                validationObligation.setExecutable(injector.getInstance(TraceViewHandler.class).getTraces().stream()
                        .filter(item -> item.getLocation().toString().equals(validationObligation.getItem()))
                        .findAny()
                        .orElse(null));
                break;
            default:
                throw new RuntimeException("Validation task is invalid: " + validationObligation.getTask());
        }
    }

    public void showPossibleLinkings(Menu linkItem, Requirement requirement) {
        linkItem.getItems().clear();
        List<Observable> dependentProperties = dependentPropertiesFromRequirement(requirement);
        for(Observable observable : dependentProperties) {
            if(observable instanceof SetProperty) {
                ((SetProperty<?>) observable).forEach(obj -> createLinkingItem(linkItem, requirement, obj));
            } else if(observable instanceof ListProperty) {
                ((ListProperty<?>) observable).forEach(obj -> createLinkingItem(linkItem, requirement, obj));
            }
        }
    }

    private void createLinkingItem(Menu linkItem, Requirement requirement, Object voExecutable) {
        MenuItem voItem = new MenuItem(generateVOName(voExecutable));
        voItem.setOnAction(e -> {
            ValidationObligation validationObligation = linkValidationObligation(voExecutable);
            requirement.addValidationObligation(validationObligation);
            validationObligation.checkedProperty().addListener((o, from, to) -> requirement.updateChecked());
            voChecker.check(validationObligation);
        });
        linkItem.getItems().add(voItem);
    }

    private String generateVOName(Object item) {
        if(item instanceof ModelCheckingItem) {
            return String.format("MC(%s)", ((ModelCheckingItem) item).getOptions().getPrologOptions().stream().map(Enum::toString).collect(Collectors.joining(", ")));
        } else if(item instanceof LTLFormulaItem) {
            return String.format("LTL(%s)", ((LTLFormulaItem) item).getCode());
        } else if(item instanceof SymbolicCheckingFormulaItem) {
            // TODO: Implement
            return null;
        } else if(item instanceof ReplayTrace) {
            return String.format("TR(%s)", ((ReplayTrace) item).getName());
        } else {
            throw new RuntimeException("Validation item is not valid. Class is: " + item.getClass());
        }
    }

    public void updateLinkingListener(Menu linkItem, Requirement from, Requirement to, InvalidationListener linkingListener) {
        if (from != null) {
            List<Observable> dependentProperties = dependentPropertiesFromRequirement(from);
            for (Observable observable : dependentProperties) {
                observable.removeListener(linkingListener);
            }
        }

        if(to != null) {
            List<Observable> dependentProperties = dependentPropertiesFromRequirement(to);
            Observable[] dependentPropertiesAsArray = dependentProperties.toArray(new Observable[0]);
            BooleanBinding emptyProperty = Bindings.createBooleanBinding(() -> {
                boolean result = true;
                for(Observable observable : dependentPropertiesAsArray) {
                    if(observable instanceof SetProperty) {
                        result = result && ((SetProperty<?>) observable).emptyProperty().get();
                    } else if(observable instanceof ListProperty) {
                        result = result && ((ListProperty<?>) observable).emptyProperty().get();
                    }
                }
                return result;
            }, dependentPropertiesAsArray);

            for (Observable observable : dependentProperties) {
                linkItem.disableProperty().bind(emptyProperty);
                observable.addListener(linkingListener);
            }

            linkingListener.invalidated(null);
        }
    }

    private List<Observable> dependentPropertiesFromRequirement(Requirement requirement) {
        RequirementType requirementType = requirement.getType();
        List<Observable> lists = new ArrayList<>();
        Machine machine = currentProject.getCurrentMachine();
        switch (requirementType) {
            case INVARIANT:
                lists.add(machine.modelcheckingItemsProperty());
                lists.add(machine.ltlFormulasProperty());
                lists.add(machine.symbolicCheckingFormulasProperty());
                break;
            case SAFETY:
                lists.add(machine.ltlFormulasProperty());
                break;
            case LIVENESS:
                lists.add(machine.ltlFormulasProperty());
                break;
            case USE_CASE:
                lists.add(injector.getInstance(TraceViewHandler.class).getTraces());
                break;
            default:
                throw new RuntimeException("Requirement type is invalid: " + requirementType);
        }
        return lists;
    }
}
