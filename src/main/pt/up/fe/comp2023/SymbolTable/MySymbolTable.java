package pt.up.fe.comp2023.SymbolTable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;

import java.util.*;

public class MySymbolTable implements SymbolTable {

    private final Set<String> imports = new HashSet<>();
    private final HashMap<String, MethodScope> methods = new HashMap<>();
    private final HashMap<String, Symbol> fields = new HashMap<>();
    private final List<List<String>> overloads = new ArrayList<>(); // [PLACE, TYPE, NAME]
    private String currentMethod = null;
    private String className = "", superClass = "";

    // ========================== CONSTRUCTOR ==========================

    public MySymbolTable(JmmParserResult parserResult) {
        if (parserResult == null) return;
        SymbolTableVisitor visitor = new SymbolTableVisitor(this);
        visitor.visit(parserResult.getRootNode());
    }

    public MethodScope getCurrentMethodScope() {
        return this.getMethod(this.currentMethod);
    }

    public String getCurrentMethod() {
        return this.currentMethod;
    }

    public void setCurrentMethod(String methodLabel) {
        this.currentMethod = methodLabel;
    }

    // ========================== IMPORTS ==========================

    @Override
    public List<String> getImports() {
        return imports.stream().toList();
    }

    public boolean hasImport(String importName) {
        return this.imports.contains(importName);
    }

    public void addImport(String newImport) {
        this.imports.add(newImport);
    }

    // ========================== Class ==========================

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    // ========================== SUPERCLASS ==========================

    @Override
    public String getSuper() {
        return superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public boolean hasSuperClass() {
        return !this.superClass.equals("");
    }

    // ========================== FIELDS ==========================

    @Override
    public List<Symbol> getFields() {
        return new ArrayList<>(fields.values());
    }

    public Symbol getField(String name) {
        return fields.get(name);
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    public List<List<String>> getOverloads() {
        return overloads;
    }

    public void addOverload(String place, String type, String name) {
        List<String> overload = new ArrayList<>();
        overload.add(place);
        overload.add(type);
        overload.add(name);
        overloads.add(overload);
    }

    public void addField(Symbol symbol) {
        if (hasField(symbol.getName())) {
            // Add the field to the overloads list
            String name = symbol.getType().getName() + " " + symbol.getName();
            addOverload("class", "field", name);
        } else fields.put(symbol.getName(), symbol);
    }

    public boolean isField(String fieldLabel) {
        return fields.containsKey(fieldLabel);
    }

    // ========================== METHODS ==========================

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(methods.keySet());
    }

    public MethodScope getMethod(String label) {
        return methods.get(label);
    }

    public int getMethodRegister(String method, String var) {
        if (methods.containsKey(method)) return methods.get(method).getVarRegister(var);
        else return -1;
    }

    @Override
    public Type getReturnType(String label) {
        return methods.get(label).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String label) {
        List<Symbol> parameters = methods.get(label).getParameters();
        return new ArrayList<>(parameters);
    }

    public boolean hasMethod(String label) {
        return methods.containsKey(label);
    }

    public void addMethod(String methodLabel, MethodScope methodScope) {
        if (methods.containsKey(methodLabel)) {
            // Add the field to the overloads list
            String name = methodScope.getReturnType().getName() + " " + methodLabel + "()";
            addOverload("class", "method", name);
        } else methods.put(methodLabel, methodScope);
    }

    public boolean isMethod(String methodLabel) {
        return methods.containsKey(methodLabel);
    }

    // ========================== METHOD VARIABLES ==========================

    @Override
    public List<Symbol> getLocalVariables(String methodLabel) {
        return new ArrayList<>(methods.get(methodLabel).getLocalVariables());
    }

    public void addLocalVariable(String methodLabel, Symbol symbol) {
        boolean hasVar = methods.get(methodLabel).hasLocalVariable(symbol.getName());
        boolean hasParam = methods.get(methodLabel).hasParameter(symbol.getName());

        if (hasVar || hasParam) {
            // Add the field to the overloads list
            String name = symbol.getType().getName() + " " + symbol.getName();
            addOverload("method " + methodLabel + "()", "var", name);
        } else methods.get(methodLabel).addLocalVariable(symbol);
    }

    public boolean isVariable(String variableLabel) {
        return isField(variableLabel) || isLocal(variableLabel);
    }

    private boolean isLocal(String variableLabel) {
        return getCurrentMethodScope().hasLocalVariable(variableLabel);
    }


    public SymbolOrigin getSymbolOrigin(String symbolLabel) {
        if (getCurrentMethodScope().hasParameter(symbolLabel)) // Is it a parameter?
            return SymbolOrigin.PARAMETER;
        else if (getCurrentMethodScope().hasLocalVariable(symbolLabel)) // Is it a local variable?
            return SymbolOrigin.LOCAL;
        else if (hasField(symbolLabel)) // Is it a field?
            return SymbolOrigin.FIELD;
        else if (hasImport(symbolLabel)) return SymbolOrigin.IMPORT;
        return SymbolOrigin.UNKNOWN;
    }

    // ========================== PRINT ==========================

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== Symbol Table ===============\n\n");

        sb.append("Imports:\n");
        for (String imp : imports)
            sb.append("\t").append(imp).append("\n");

        sb.append("\nClass:\n");
        sb.append("\tName: ").append(className).append("\n");
        sb.append("\tSuperclass: ").append(superClass).append("\n\n");

        sb.append("Fields:\n");
        for (Symbol field : fields.values()) {
            String type = field.getType().getName() + (field.getType().isArray() ? "[]" : "");
            sb.append("\t").append(type).append(" ").append(field.getName());
            sb.append("\n");
        }
        sb.append("\n");

        sb.append("Methods:\n");
        for (MethodScope method : methods.values()) {

            if (method.getReturnType() != null) {
                String isArr = (method.getReturnType().isArray() ? "[]" : "");
                sb.append("\t").append(method.getReturnType().getName()).append(isArr).append(" ");
            }

            sb.append(method.getMethodName()).append("(");
            List<Symbol> parameters = method.getParameters();

            for (int i = 0; i < parameters.size(); i++) {
                Symbol param = parameters.get(i);
                String type = param.getType().getName() + (param.getType().isArray() ? "[]" : "");
                sb.append(type).append(" ").append(param.getName());
                if (i != parameters.size() - 1) sb.append(", ");
            }

            sb.append(")\n\t\tVariables:\n");
            if (method.getLocalVariables().size() == 0) sb.append("\t\t\tNone\n");
            else for (Symbol variable : method.getLocalVariables()) {
                String type = variable.getType().getName() + (variable.getType().isArray() ? "[]" : "");
                sb.append("\t\t\t").append(type).append(" ").append(variable.getName()).append("\n");
            }

            sb.append("\n");
        }

        sb.append("======================================\n");

        return sb.toString();
    }

    public int curIsNotStatic() {
        return currentMethod.equals("main") ? 0 : 1;
    }

    public String getParameterIndex(String varName) {
        MethodScope methodScope = getCurrentMethodScope();
        List<Symbol> parameters = methodScope.getParameters();

        return String.valueOf(parameters.indexOf(methodScope.getParameter(varName)) + curIsNotStatic());
    }

    public Type findTypeVar(String varName) {
        return findTypeVar(varName, null);
    }

    public Type findTypeVar(String varName, JmmNode node) {
        Symbol symbol = this.getCurrentMethodScope().getLocalVariable(varName); // Is it a local variable?
        if (symbol == null) {
            symbol = this.getField(varName); // Is it a field?
        }
        if (symbol == null) {
            symbol = this.getCurrentMethodScope().getParameter(varName); // Is it a parameter ?
        }
        if (this.hasImport(varName)) { // Is it an import?
            return new Type("void", false);
        }
        if (symbol == null && this.hasSuperClass()) {
            if (node == null) {
                return new Type("i32", false); // NOTA: o professor disse que este caso nunca acontece, terá de estar sempre a ser "dito" o tipo
            }
            if (node.hasAttribute("expType")) {
                return new Type(node.get("expType"), node.get("expType").contains("["));
            }
        }

        if (symbol == null) return null;
        return symbol.getType();
    }

    public boolean findVar(String varName, String currentMethod) {
        if (getMethod(currentMethod).hasLocalVariable(varName)) return true;

        //se for um parametro
        for (Symbol parameter : getMethod(currentMethod).getParameters())
            if (parameter.getName().equals(varName)) return true;

        //se for um field
        for (Symbol field : getFields()) if (field.getName().equals(varName)) return true;

        //se for um import
        for (String imports : getImports()) if (imports.equals(varName)) return true;

        return false;
    }

    public void addConstantVar(String varName) {
        if (getMethod(currentMethod) != null) getMethod(currentMethod).addConstantVar(varName);
    }

    public void clearConstantVars(){
        if (getMethod(currentMethod) != null) getMethod(currentMethod).clearConstantVars();
    }

}

