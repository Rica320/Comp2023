package pt.up.fe.comp2023.OptimizeVisitors;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

public class ConstantFolding extends AJmmVisitor<String, String> {

    private final MySymbolTable st;

    public ConstantFolding(MySymbolTable st) {
        this.st = st;
    }

    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        return semanticsResult;
    }

    @Override
    public String visit(JmmNode jmmNode, String data) {
        return super.visit(jmmNode, data);
    }

    @Override
    protected void buildVisitor() {

        // Assign
        addVisit("Assign", this::dealWithAssign);
        addVisit("ArrayAssign", this::dealWithArrayAssign);

        // Expressions
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("NewObject", this::dealWithNewObject);

        // Array
        addVisit("ArrayLookup", this::dealWithArrayLookup);
        addVisit("NewIntArray", this::dealWithNewIntArray);

        // Binary ops
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("BinaryComp", this::dealWithBinaryOp);
        addVisit("BinaryBool", this::dealWithBinaryOp);

        addVisit("Not", this::dealWithNot);

        // Literals and variables
        addVisit("Var", this::dealWithVar);
        addVisit("Boolean", this::dealWithBoolean);
        addVisit("Int", this::dealWithInt);

        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithArrayAssign(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithAssign(JmmNode jmmNode, String s) {
        return "";
    }

    private String defaultVisit(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren())
            visit(child, s);
        return "";
    }


    private String dealWithArrayLookup(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithAttributeAccess(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithMethodCall(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithNot(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithNewIntArray(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithNewObject(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithThis(JmmNode jmmNode, String s) {
        return "";
    }


    private String dealWithVar(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithBoolean(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithInt(JmmNode jmmNode, String s) {
        return "";
    }
}
