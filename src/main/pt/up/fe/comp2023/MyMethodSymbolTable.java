package pt.up.fe.comp2023;


import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MyMethodSymbolTable {
    String methodName;
    List<MySymbol> parameters;
    Type returnType;
    HashMap<String, MySymbol> localVariables = new HashMap<>();


    // ========================== CONSTRUCTORS ==========================

    public MyMethodSymbolTable(Type returnT, String name, List<MySymbol> MethodParameters) {
        methodName = name;
        returnType = returnT;
        parameters = MethodParameters;
    }

    // ========================== GETTERS ==========================

    public String getMethodName() {
        return methodName;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<MySymbol> getParameters() {
        return parameters;
    }

    public MySymbol getParameter(String name) {


        for (MySymbol p : parameters)
            if (p.getName().equals(name))
                return p;
        return null;
    }

    public List<MySymbol> getLocalVariables() {
        return new ArrayList<MySymbol>(localVariables.values());
    }

    public MySymbol getLocalVariable(String variableName) {
        return localVariables.get(variableName);
    }

    // ========================== SETTERS ==========================

    public Boolean addLocalVariable(MySymbol var) {
        if (localVariables.containsKey(var.getName()))
            return false; // already exists
        localVariables.put(var.getName(), var);
        return true;
    }

    public void setLocalVariableValue(String name, String value) {
        localVariables.get(name).setValue(value);
    }

    public boolean assignVariable(MySymbol var) {
        if (localVariables.containsKey(var.getName()))
            return false; // already exists
        localVariables.put(var.getName(), var);
        return true;
    }

}