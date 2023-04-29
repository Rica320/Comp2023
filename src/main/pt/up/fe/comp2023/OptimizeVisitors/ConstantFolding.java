package pt.up.fe.comp2023.OptimizeVisitors;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

public class ConstantFolding extends AJmmVisitor<String, String> {

    private boolean changed = false;

    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        changed = false;
        JmmNode root = semanticsResult.getRootNode();
        visit(root, "");
        return semanticsResult;
    }

    private void replaceParent(JmmNode oldNode, JmmNode newNode) {
        JmmNode parent = oldNode.getJmmParent();
        if (parent == null) {
            System.out.println("Parent is null");
            return;
        }
        int index = parent.getChildren().indexOf(oldNode);
        parent.setChild(newNode, index);
        this.changed = true;
        try {
            System.out.println("Folded constant " + oldNode.getJmmChild(0).get("val") + " " + oldNode.get("op") + " " + oldNode.getJmmChild(1).get("val") + " = " + newNode.get("val"));
        } catch (Exception e) {
            System.out.println("Removed parentesis");
        }
    }

    public boolean isChanged() {
        return this.changed;
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
        addVisit("Paren", this::dealWithParen);
        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithParen(JmmNode jmmNode, String s) {
        // if it only has one child, replace it with the child
        if (jmmNode.getChildren().size() == 1) replaceParent(jmmNode, jmmNode.getJmmChild(0));
        return "";
    }

    private String defaultVisit(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren())
            visit(child, s);
        return "";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);

        // Visit left and right because they might be expressions that can be folded themselves
        visit(left, s);
        visit(right, s);

        // Check if left and right are literals
        if (left.getKind().equals("Int") || left.getKind().equals("Boolean")) {
            int leftValue, rightValue;
            try {
                leftValue = Integer.parseInt(left.get("val"));
                rightValue = Integer.parseInt(right.get("val"));
            } catch (Exception e) {
                throw new RuntimeException("ERROR: " + left + " or " + right + " is not a literal");
            }

            String op = jmmNode.get("op");
            int result;

            switch (op) {
                case "+" -> result = leftValue + rightValue;
                case "-" -> result = leftValue - rightValue;
                case "*" -> result = leftValue * rightValue;
                case "/" -> result = leftValue / rightValue;
                case "<" -> result = leftValue < rightValue ? 1 : 0;
                case "&&" -> result = leftValue == 1 && rightValue == 1 ? 1 : 0;
                default -> {
                    System.out.println("Unknown operator: " + op);
                    return "";
                }
            }

            String kind = (op.equals("&&") || op.equals("<")) ? "Boolean" : "Int";
            JmmNode newNode = new JmmNodeImpl(kind);
            newNode.put("val", String.valueOf(result));
            replaceParent(jmmNode, newNode);
        }
        return "";
    }
}
