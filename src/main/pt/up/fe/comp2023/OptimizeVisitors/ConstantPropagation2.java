package pt.up.fe.comp2023.OptimizeVisitors;

import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Triple;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConstantPropagation2 extends AJmmVisitor<String, String> {

    private final List<Pair<String, Triple<String, Integer, Integer>>> vars = new ArrayList<>(); // varName, varKind, varScope, varValue
    Set<String> constantVars = new HashSet<>();
    MySymbolTable st;
    private boolean changed = false;
    private int scope = 0;

    public ConstantPropagation2(MySymbolTable st) {
        this.st = st;
    }

    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        changed = false;
        JmmNode root = semanticsResult.getRootNode();
        visit(root, "");
        return semanticsResult;
    }

    private void replaceNode(JmmNode oldNode, JmmNode newNode) {
        System.out.println("Replacing " + oldNode + " with " + newNode);
        JmmNode parent = oldNode.getJmmParent();
        if (parent == null) return;
        int index = parent.getChildren().indexOf(oldNode);
        parent.setChild(newNode, index);
        this.changed = true;
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
        addVisit("MainMethod", this::dealWithMain);
        addVisit("MethodDecl", this::dealWithMethod);
        addVisit("Var", this::dealWithVar);
        addVisit("Assign", this::dealWithAssign);
        addVisit("VarDcl", this::dealWithVarDcl);
        addVisit("Scope", this::dealWithScope);
        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithScope(JmmNode jmmNode, String s) {
        scope++;
        defaultVisit(jmmNode, s);
        scope--;
        return null;
    }

    private String defaultVisit(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) visit(child, s);
        return null;
    }

    private String dealWithMethod(JmmNode jmmNode, String s) {
        st.setCurrentMethod(jmmNode.get("name"));
        scope = 0;
        defaultVisit(jmmNode, s);
        vars.clear();
        constantVars.clear();
        st.setCurrentMethod(null);
        return null;
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        st.setCurrentMethod("main");
        scope = 0;
        defaultVisit(jmmNode, s);
        vars.clear();
        constantVars.clear();
        st.setCurrentMethod(null);
        return null;
    }

    private void removeNode(JmmNode node) {
        JmmNode parent = node.getJmmParent();
        if (parent == null) return;
        parent.removeJmmChild(node);
        st.getCurrentMethodScope().removeLocalVar(node.get("var")); // Remove the variable from the symbol table as well
        this.changed = true;
    }

    private String dealWithVarDcl(JmmNode jmmNode, String s) {
        String varName = jmmNode.get("var");

        //  If the variable is not a constant, then it is safe to remove it
        if (constantVars.contains(varName)) removeNode(jmmNode);
        return null;
    }


    private String dealWithVar(JmmNode jmmNode, String s) {
        System.out.println(vars);
        System.out.println("Var: " + jmmNode.get("var"));

        // check if variable is in the list
        String varname = jmmNode.get("var");
        for (Pair<String, Triple<String, Integer, Integer>> var : vars)
            if (var.a.equals(varname)) {
                // replace node with constant
                JmmNode newNode = new JmmNodeImpl(var.b.a); // var.b.a = varKind
                newNode.put("val", var.b.c.toString()); // var.b.c = varValue
                replaceNode(jmmNode, newNode);
                break;
            }

        return null;
    }

    private String dealWithAssign(JmmNode jmmNode, String s) {

        // Add all variables assigned to a constant to the list
        String kind = jmmNode.getJmmChild(0).getKind();
        if (kind.equals("Int") || kind.equals("Boolean") && scope == 0) {
            String varname = jmmNode.get("var");
            int value = Integer.parseInt(jmmNode.getJmmChild(0).get("val"));

            // Remove previous assignments to the same variable
            for (int i = 0; i < vars.size(); i++) {
                Pair<String, Triple<String, Integer, Integer>> var = vars.get(i);
                if (var.a.equals(varname)) {
                    vars.remove(i);
                    break;
                }
            }

            constantVars.add(varname);
            vars.add(new Pair<>(varname, new Triple<>(kind, scope, value)));
            removeNode(jmmNode);
        }

        return null;
    }

}
