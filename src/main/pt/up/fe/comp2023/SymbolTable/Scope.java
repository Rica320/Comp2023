package pt.up.fe.comp2023.SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Scope {
    int depth;
    Scope parentScope = null;
    HashMap<String, MySymbol> localVariables = new HashMap<>();

    // ========================== CONSTRUCTOR ==========================

    public Scope(int depth, Scope parentScope) {
        this.depth = depth;
        this.parentScope = parentScope;
    }

    // ========================== GETTERS ==========================

    public List<MySymbol> getLocalVariables() {
        return new ArrayList<MySymbol>(localVariables.values());
    }

    public MySymbol getLocalVariable(String variableName) {
        return localVariables.get(variableName);
    }

    // ========================== SETTERS ==========================

    public Boolean addLocalVariable(MySymbol var) {
        if (localVariables.containsKey(var.getName())) return false; // already exists
        localVariables.put(var.getName(), var);
        return true;
    }

    public void setLocalVariableValue(String name, String value) {
        localVariables.get(name).setValue(value);
    }

    public boolean assignVariable(MySymbol var) {
        if (localVariables.containsKey(var.getName())) return false; // already exists
        localVariables.put(var.getName(), var);
        return true;
    }

    public boolean hasLocalVariable(String variableName) {
        return localVariables.containsKey(variableName);
    }

}
