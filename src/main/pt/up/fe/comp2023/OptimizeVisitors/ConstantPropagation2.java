package pt.up.fe.comp2023.OptimizeVisitors;

import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Triple;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.*;

public class ConstantPropagation2 extends AJmmVisitor<String, String> {

    private final HashMap<String, Triple<String, Integer, Integer>> vars = new HashMap<>();  // varName : varKind, varScope, varValue
    Set<String> scopedAssignedVars = new HashSet<>();
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
        scopedAssignedVars.clear();
        st.setCurrentMethod(null);
        return null;
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        st.setCurrentMethod("main");
        scope = 0;
        defaultVisit(jmmNode, s);
        vars.clear();
        scopedAssignedVars.clear();
        st.setCurrentMethod(null);
        return null;
    }

/*    private void removeNode(JmmNode node) {
        JmmNode parent = node.getJmmParent();
        if (parent == null) return;
        parent.removeJmmChild(node);
        st.getCurrentMethodScope().removeLocalVar(node.get("var"));
        this.changed = true;
    }*/

    private String dealWithVarDcl(JmmNode jmmNode, String s) {
        String varName = jmmNode.get("var");
        return null; // TODO: REMOVER MÉTODO
    }

    private String dealWithVar(JmmNode jmmNode, String s) {

        // check if variable is in the list
        String varname = jmmNode.get("var");

        if (vars.containsKey(varname)) {
            if (vars.get(varname).b == 0 && !scopedAssignedVars.contains(varname)) { // scope == 0
                Triple<String, Integer, Integer> var = vars.get(varname);
                JmmNode newNode = new JmmNodeImpl(var.a, jmmNode);
                newNode.put("val", var.c.toString());
                replaceNode(jmmNode, newNode);
            } else {

            }
        }

        return null;
    }

    private String dealWithAssign(JmmNode jmmNode, String s) {

        // Add all variables assigned to a constant to the list
        String kind = jmmNode.getJmmChild(0).getKind();
        if (kind.equals("Int") || kind.equals("Boolean")) {
            String varname = jmmNode.get("var");
            int value = Integer.parseInt(jmmNode.getJmmChild(0).get("val"));

            if (scope == 0) {
                vars.put(varname, new Triple<>(kind, scope, value));
            } else { // scope > 0

            }
        }

        return null;
    }

}
