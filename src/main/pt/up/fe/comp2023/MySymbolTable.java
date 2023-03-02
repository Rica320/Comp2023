package pt.up.fe.comp2023;

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
• Include type in each symbol (e.g. a local variable “a” is of type X. Also, is “a” array?)
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

/* BUILT IN
public Symbol(Type type, String name) {
    this.type = type;
    this.name = name;
}
*/

public class MySymbolTable implements SymbolTable {

    private Set<String> imports = new HashSet<>();
    private String className = "", superClass = "";
    private HashMap<String, MethodTable> methods = new HashMap<>();
    private HashMap<String, MySymbol> fields = new HashMap<>();

    private MySymbolTable parent;

    // ========================== CONSTRUCTORS ==========================

    public MySymbolTable() {
        this.parent = null;
    }

    public MySymbolTable(MySymbolTable parent) {
        this.parent = parent;
    }

    // ========================== PARENT ==========================

    public MySymbolTable getParent() {
        return parent;
    }

    public void setParent(MySymbolTable parent) {
        this.parent = parent;
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

    public Boolean addField(Symbol symbol, String value) {
        if (hasField(symbol.getName()))
            return false; // already exists
        fields.put(symbol.getName(), new MySymbol(symbol, value));
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

    public MethodTable getMethod(String label) {
        return methods.get(label);
    }

    @Override
    public Type getReturnType(String label) {
        return methods.get(label).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String label) {
        List<Parameter> parameters = methods.get(label).getParameters();
        List<Symbol> symbolsPar = new ArrayList<>();

        for (Parameter p : parameters) {
            symbolsPar.add(p.getSymbol());
        }
        return symbolsPar;
    }

    public boolean hasMethod(String label) {
        return methods.containsKey(label);
    }

    public void addMethod(String methodLabel, MethodTable methodTable) {
        this.methods.put(methodLabel, methodTable);
    }

    public boolean isMethod(String methodLabel) {
        return methods.containsKey(methodLabel);
    }

    // ========================== METHOD VARIABLES ==========================

    @Override
    public List<Symbol> getLocalVariables(String label) {
        return new ArrayList<>(methods.get(label).getLocalVariables().keySet());
    }

    // ========================== OTHERS ==========================

    public boolean isVariable(String variableLabel) {
        return isField(variableLabel) || isMethod(variableLabel);
    }

    public void assignMethodVariable(String methodLabel, String var) {
        // shouldn't it be a MySymbol instead?
        methods.get(methodLabel).assignVariable(var);
    }

    // ========================== PRINT ==========================

    @Override
    public String toString() {

        return "Imports: " + imports + "\n" +
                "Class: " + className + "\n" +
                "Super: " + superClass + "\n" +
                "Fields: " + fields + "\n" +
                "Methods: " + methods + "\n";

    }
}







