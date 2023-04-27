package pt.up.fe.comp2023.OptimizeVisitors;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

public class ConstantPropagation extends AJmmVisitor<String, String> {

    private final MySymbolTable st;
    private boolean changed = false;

    public ConstantPropagation(MySymbolTable st) {
        this.st = st;
    }

    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        JmmNode root = semanticsResult.getRootNode();
        visit(root, "");
        return semanticsResult;
    }

    private void replaceParent(JmmNode oldNode, JmmNode newNode) {
        JmmNode parent = oldNode.getJmmParent();
        if (parent == null) return;
        int index = parent.getChildren().indexOf(oldNode);
        parent.setChild(newNode, index);
        this.changed = true;
    }

    public boolean isChanged() {
        return this.changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    @Override
    public String visit(JmmNode jmmNode, String data) {
        return super.visit(jmmNode, data);
    }

    @Override
    protected void buildVisitor() {

        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("BinaryComp", this::dealWithBinaryOp);
        addVisit("BinaryBool", this::dealWithBinaryOp);

        //addVisit("Assign", this::dealWithAssign);
        //addVisit("ArrayAssign", this::dealWithArrayAssign);
        //addVisit("MethodCall", this::dealWithMethodCall);
        //addVisit("NewObject", this::dealWithNewObject);
        // addVisit("Not", this::dealWithNot);
        //addVisit("Var", this::dealWithVar);
        //addVisit("Boolean", this::dealWithBoolean);
        //addVisit("Int", this::dealWithInt);
        //addVisit("ArrayLookup", this::dealWithArrayLookup);
        //addVisit("NewIntArray", this::dealWithNewIntArray);

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
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);

        // Check if left and right are literals
        if (left.getKind().equals("Int") || left.getKind().equals("Boolean")) {
            int leftValue = Integer.parseInt(left.get("val"));
            int rightValue = Integer.parseInt(right.get("val"));

            String op = jmmNode.get("op");
            int result = 0;

            switch (op) {
                case "+" -> result = leftValue + rightValue;
                case "-" -> result = leftValue - rightValue;
                case "*" -> result = leftValue * rightValue;
                case "/" -> result = leftValue / rightValue;
                case "<" -> result = leftValue < rightValue ? 1 : 0;
                case "&&" -> result = leftValue == 1 && rightValue == 1 ? 1 : 0;
                default -> {
                }
            }

            JmmNode newNode = new JmmNodeImpl(left.getKind());
            newNode.put("val", String.valueOf(result));
            replaceParent(jmmNode, newNode);
        }
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
