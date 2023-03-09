package pt.up.fe.comp2023.SymbolTable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.Visitor.MyNodeVisitor;

import java.util.*;

/*
• Imported classes
• Declared class (and its extension)
• Fields inside the declared class
• Methods inside the declared class
• Parameters and return type for each method
• Local variables for each method
• Include type in each symbol (e.g. a local variable "a" is of type X. Also, is "a" array?)
• Print Symbol Table
• Used interfaces: SymbolTable, AJmmVisitor (the latter is optional)
*/


/*

You are free to implement the SymbolTable as you see fit. One viable way to implement it is to use
several Map instances internally to map information between a method label, and information
related to that method (e.g., parameters, return type). Another way is to create a class that stores
information related to just a single method and use a single Map that maps labels to instances
of that class.

no overloading => method as its label is sufficient

To build the symbol table you will need to get the information from the AST. You can either visit the
AST manually (e.g., code that checks the type of the node and visits the children) or use the Visitor
pattern (e.g. extend AJmmVisitor ). Since the language is relatively simple, the analysis does not
need to go very deep in the AST, and both approaches are viable.
*/

public class MySymbolTable implements SymbolTable {

    private String currentMethod = null;
    private final Set<String> imports = new HashSet<>();
    private String className = "", superClass = "";
    private final HashMap<String, MethodScope> methods = new HashMap<>();
    private final HashMap<String, Symbol> fields = new HashMap<>();

    // ========================== CONSTRUCTOR ==========================

    public MySymbolTable() {
    }

    public MySymbolTable(String className, String superClass) {
        this.className = className;
        this.superClass = superClass;
    }

    public MySymbolTable populateSymbolTable(JmmParserResult parserResult) {
        MyNodeVisitor visitor = new MyNodeVisitor(this);
        visitor.visit(parserResult.getRootNode());
        return this;
    }

    public MethodScope getCurrentMethodScope() {
        return this.getMethod(this.currentMethod);
    }

    public void setCurrentMethod(String methodLabel) {
        this.currentMethod = methodLabel;
    }

    public String getCurrentMethod() {
        return this.currentMethod;
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

    public Boolean addField(Symbol symbol) {
        if (hasField(symbol.getName())) return false; // already exists
        fields.put(symbol.getName(), symbol);
        return true;
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
        this.methods.put(methodLabel, methodScope);
    }

    public boolean isMethod(String methodLabel) {
        return methods.containsKey(methodLabel);
    }

    // ========================== METHOD VARIABLES ==========================

    @Override
    public List<Symbol> getLocalVariables(String methodLabel) {
        return new ArrayList<>(methods.get(methodLabel).getLocalVariables());
    }

    // ========================== OTHERS ==========================

    public boolean isVariable(String variableLabel) {
        return isField(variableLabel) || isMethod(variableLabel);
    }

    public void assignMethodVariable(String methodLabel, Symbol symbol) {
        methods.get(methodLabel).assignVariable(symbol);
    }

    // ========================== PRINT ==========================

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== Symbol Table ===============\n\n");

        sb.append("Imports:\n");
        for (String imp : imports) {
            sb.append("\t").append(imp).append("\n");
        }
        sb.append("\n");

        sb.append("Class:\n");
        sb.append("\tName: ").append(className).append("\n");
        sb.append("\tSuperclass: ").append(superClass).append("\n\n");

        sb.append("Fields:\n");
        for (Symbol field : fields.values()) {
            sb.append("\t").append(field.getName()).append(" (").append(field.getType()).append(")");
            // if (field.isArray()) {
            //     sb.append("[]");
            // }
            // if (field.getValue() != null) {
            //     sb.append(" = ").append(field.getValue());
            // }
            sb.append("\n");
        }
        sb.append("\n");

        sb.append("Methods:\n");
        for (MethodScope method : methods.values()) {
            sb.append("\t").append(method.getMethodName()).append("(");
            List<Symbol> parameters = method.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                Symbol param = parameters.get(i);
                sb.append(param.getName()).append(":").append(param.getType());
                // if (param.isArray()) {
                //     sb.append("[]");
                // }
                if (i != parameters.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            if (method.getReturnType() != null) {
                sb.append(": ").append(method.getReturnType());
            }
            sb.append("\n");

            sb.append("\t\tVariables:\n");
            for (Symbol variable : method.getLocalVariables()) {
                sb.append("\t\t\t").append(variable.getName()).append(" (").append(variable.getType()).append(")");
                // if (variable.isArray()) {
                //     sb.append("[]");
                // }
                // if (variable.getValue() != null) {
                //     sb.append(" = ").append(variable.getValue());
                // }
                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("======================================\n");

        return sb.toString();
    }
}







