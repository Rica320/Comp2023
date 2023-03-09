package pt.up.fe.comp2023.SymbolTable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Scope {
    int depth;
    Scope parentScope = null;
    HashMap<String, Symbol> localVariables = new HashMap<>();

    // ========================== CONSTRUCTOR ==========================

    public Scope(int depth, Scope parentScope) {
        this.depth = depth;
        this.parentScope = parentScope;
    }

    // ========================== GETTERS ==========================

    public List<Symbol> getLocalVariables() {
        return new ArrayList<Symbol>(localVariables.values());
    }

    public Symbol getLocalVariable(String variableName) {
        return localVariables.get(variableName);
    }

    // ========================== SETTERS ==========================

    public Boolean addLocalVariable(Symbol var) {
        if (localVariables.containsKey(var.getName())) return false; // already exists
        localVariables.put(var.getName(), var);
        return true;
    }

    public boolean assignVariable(Symbol var) {
        if (localVariables.containsKey(var.getName())) return false; // already exists
        localVariables.put(var.getName(), var);
        return true;
    }

    public boolean hasLocalVariable(String variableName) {
        return localVariables.containsKey(variableName);
    }

}
