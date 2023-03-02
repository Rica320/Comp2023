package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

//Represents a variable (field or a parameter)
public class MySymbol extends Symbol {

    private boolean isAssigned = false;
    private int order = -1; // -1 if is not a parameter
    private String value = "";
    // Symbol Type already contains if is array or not

    // ========================== CONSTRUCTORS ==========================

    public MySymbol(Symbol symbol, String value) {
        super(symbol.getType(), symbol.getName());
    }

    public MySymbol(Type type, String name) {
        super(type, name);
    }

    public MySymbol(Type type, String name, String value) {
        super(type, name);
        isAssigned = true;
    }

    // ========================== GETTERS / SETTERS ==========================

    public boolean isAssigned() {
        return isAssigned;
    }

    public void setValue(String var_value) {
        isAssigned = true;
        // TO-DO: check if value is valid
        value = var_value;
    }

    public String getValue() {
        return value;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isParameter() {
        return order != -1;
    }

}
