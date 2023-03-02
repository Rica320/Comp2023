package pt.up.fe.comp2023.SymbolTable;

import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class MethodScope {
    String methodName;
    List<MySymbol> parameters;
    Type returnType;
    Scope currentScope = new Scope(0, null);
    int depth = 0;

    // ========================== CONSTRUCTOR ==========================

    public MethodScope(Type returnT, String name, List<MySymbol> MethodParameters) {
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

    public void newScope() {
        currentScope = new Scope(++depth, currentScope);
    }

    public void endScope() {
        currentScope = currentScope.parentScope;
        depth--;
    }

    public List<MySymbol> getLocalVariables() {
        return currentScope.getLocalVariables();
    }

    public MySymbol getLocalVariable(String variableName) {
        return currentScope.getLocalVariable(variableName);
    }

    public boolean hasLocalVariable(String variableName) {
        return currentScope.hasLocalVariable(variableName);
    }

    public void setLocalVariableValue(String name, String value) {
        currentScope.setLocalVariableValue(name, value);
    }

    public boolean assignVariable(MySymbol var) {
        return currentScope.assignVariable(var);
    }

    public boolean addLocalVariable(MySymbol var) {
        return currentScope.addLocalVariable(var);
    }

    public boolean isParameter(String parameterLabel) {
        for (MySymbol p : parameters)
            if (p.getName().equals(parameterLabel))
                return true;
        return false;
    }

    public boolean isLocalVariable(String variableLabel) {
        return currentScope.hasLocalVariable(variableLabel);
    }

    public boolean isLocalVariableInScope(String variableLabel) {
        Scope scope = currentScope;
        while (scope != null) {
            if (scope.hasLocalVariable(variableLabel))
                return true;
            scope = scope.parentScope;
        }
        return false;
    }

}