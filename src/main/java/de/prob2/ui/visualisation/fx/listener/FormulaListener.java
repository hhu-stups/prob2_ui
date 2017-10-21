package de.prob2.ui.visualisation.fx.listener;

/**
 * Description of class
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 26.09.17
 */
public abstract class FormulaListener {

    private final String[] formulas;

    public FormulaListener(String formula, String... formulas) {
        if (formulas == null) {
            this.formulas = new String[]{formula};
        } else {
            this.formulas = new String[1 + formulas.length];
            this.formulas[0] = formula;
            System.arraycopy(formulas, 0, this.formulas, 1, formulas.length);
        }
    }

    public String[] getFormulas() {
        return formulas;
    }

    //TODO: Formulas as varargs of the constructor
    public abstract void variablesChanged(Object[] newValues) throws Exception;

}
