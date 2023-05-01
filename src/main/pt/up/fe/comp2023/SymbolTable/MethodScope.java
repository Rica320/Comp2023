package pt.up.fe.comp2023.SymbolTable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MethodScope {
    private final List<String> constVarList = new ArrayList<>(); // Variables that are being assigned to
    String methodName;
    List<Symbol> parameters;
    Type returnType;
    HashMap<String, Symbol> localVariables = new HashMap<>();

    // ========================== CONSTRUCTOR ==========================

    public MethodScope(Type returnT, String name, List<Symbol> MethodParameters) {
        methodName = name;
        returnType = returnT;
        parameters = Objects.requireNonNullElseGet(MethodParameters, List::of);
    }

    // ========================== GETTERS / SETTERS ==========================

    public String getMethodName() {
        return methodName;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public void setParameters(List<Symbol> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(Symbol parameter) {
        this.parameters.add(parameter);
    }

    public Symbol getParameter(String name) {
        for (Symbol p : parameters)
            if (p.getName().equals(name)) return p;
        return null;
    }

    public boolean isParameter(String parameterLabel) {
        for (Symbol p : parameters)
            if (p.getName().equals(parameterLabel)) return true;
        return false;
    }


    public List<Symbol> getLocalVariables() {
        return new ArrayList<>(localVariables.values());
    }

    public Symbol getLocalVariable(String variableName) {
        return localVariables.get(variableName);
    }


    public void addLocalVariable(Symbol var) {
        if (localVariables.containsKey(var.getName())) return; // already exists
        localVariables.put(var.getName(), var);
    }

    public boolean assignVariable(Symbol var) {
        if (localVariables.containsKey(var.getName())) return false; // already exists
        localVariables.put(var.getName(), var);
        return true;
    }

    public boolean hasLocalVariable(String variableName) {
        return localVariables.containsKey(variableName);
    }


    // ========================== PRINT ==========================

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Method: " + methodName + " (");
        for (Symbol p : parameters)
            s.append(p).append(", ");
        s.append(") -> ").append(returnType.toString()).append(" {");
        for (Symbol v : this.getLocalVariables())
            s.append(v).append(", ");
        s.append("}");
        return s + "\n\n";
    }

    public boolean hasParameter(String name) {
        for (Symbol p : parameters)
            if (p.getName().equals(name)) return true;
        return false;
    }

    public int getVarRegister(String name) {
        for (int i = 0; i < parameters.size(); i++)
            if (parameters.get(i).getName().equals(name)) return i;

        var vars = getLocalVariables();
        for (int i = 0; i < vars.size(); i++)
            if (vars.get(i).getName().equals(name)) return i + parameters.size() - 1;

        return -1;
    }

    public void addConstantVar(String varName) {
        constVarList.add(varName);
    }

    public List<String> getConstantVars() {
        HashMap<String, Integer> frequency = new HashMap<>();

        // Count frequency of each value
        for (String s : constVarList)
            frequency.put(s, frequency.getOrDefault(s, 0) + 1);

        // Add values with frequency of 1 to new list
        List<String> clearList = new ArrayList<>();
        for (HashMap.Entry<String, Integer> entry : frequency.entrySet())
            if (entry.getValue() == 1) clearList.add(entry.getKey());

        return clearList;
    }

    public void clearConstantVars() {
        constVarList.clear();
    }
}