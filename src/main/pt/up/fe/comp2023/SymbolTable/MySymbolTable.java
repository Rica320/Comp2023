package pt.up.fe.comp2023.SymbolTable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

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

    public String currentMethod = null;
    private Set<String> imports = new HashSet<>();
    private String className = "", superClass = "";
    private HashMap<String, MethodScope> methods = new HashMap<>();
    private HashMap<String, MySymbol> fields = new HashMap<>();

    // ========================== CONSTRUCTOR ==========================

    public MySymbolTable() {
    }

    public MySymbolTable(String className, String superClass) {
        this.className = className;
        this.superClass = superClass;
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

    public MySymbol getField(String name) {
        return fields.get(name);
    }

    public void setFieldValue(String name, String value) {
        fields.get(name).setValue(value);
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    public Boolean addField(MySymbol symbol) {
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
        List<MySymbol> parameters = methods.get(label).getParameters();
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

    public void assignMethodVariable(String methodLabel, MySymbol symbol) {
        methods.get(methodLabel).assignVariable(symbol);
    }

    // ========================== PRINT ==========================

    @Override
    public String toString() {

        return "\n\n========== Table ===============\n" + "Imports: " + imports + "\n" + "Class: " + className + "\n" + "Super: " + superClass + "\n" + "Fields: " + fields + "\n" + "Methods: \n\n" + methods + "\n" + "===============================\n\n";

    }
}







