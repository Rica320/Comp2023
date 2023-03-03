package pt.up.fe.comp2023.SymbolTable;

import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        parameters = Objects.requireNonNullElseGet(MethodParameters, List::of);
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
            if (p.getName().equals(name)) return p;
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
            if (p.getName().equals(parameterLabel)) return true;
        return false;
    }

    public boolean isLocalVariable(String variableLabel) {
        return currentScope.hasLocalVariable(variableLabel);
    }

    public MySymbol isLocalVariableInScope(String variableLabel) {
        Scope scope = currentScope;
        while (scope != null) {
            if (scope.hasLocalVariable(variableLabel)) return scope.getLocalVariable(variableLabel);
            scope = scope.parentScope;
        }
        return null;
    }

    public List<MySymbol> getAllScopeVars() {
        Scope scope = currentScope;
        List<MySymbol> vars = new ArrayList<>();
        while (scope != null) {
            vars.addAll(scope.getLocalVariables());
            scope = scope.parentScope;
        }
        return vars;
    }

    // ========================== PRINT ==========================

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Method: " + methodName + " (");
        for (MySymbol p : parameters)
            s.append(p.toString()).append(", ");
        s.append(") -> ").append(returnType.toString()).append(" {");
        for (MySymbol v : currentScope.getLocalVariables())
            s.append(v.toString()).append(", ");
        s.append("}");
        return s.toString() + "\n\n";
    }

}