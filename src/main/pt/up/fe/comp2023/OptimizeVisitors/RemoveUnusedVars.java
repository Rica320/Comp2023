package pt.up.fe.comp2023.OptimizeVisitors;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.ArrayList;
import java.util.List;

public class RemoveUnusedVars extends AJmmVisitor<String, String> {

    MySymbolTable st;
    private boolean changed = false;
    private List<String> constantVars = new ArrayList<>(); // Variables that are only assigned constants can to be removed

    public RemoveUnusedVars(MySymbolTable st) {
        this.st = st;
    }

    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        changed = false;
        JmmNode root = semanticsResult.getRootNode();
        visit(root, "");
        return semanticsResult;
    }

    private void removeNode(JmmNode node) {
        JmmNode parent = node.getJmmParent();
        if (parent == null) return;
        parent.removeJmmChild(node);
        st.getCurrentMethodScope().removeLocalVar(node.get("var")); // Remove the variable from the symbol table as well
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
        addVisit("VarDcl", this::dealWithVarDcl);
        addVisit("Assign", this::dealWithAssign);
        setDefaultVisit(this::defaultVisit);
    }

    private String defaultVisit(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) visit(child, s);
        return null;
    }

    private String dealWithMethod(JmmNode jmmNode, String s) {
        st.setCurrentMethod(jmmNode.get("name"));
        this.constantVars = st.getCurrentMethodScope().getConstantVars();
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        this.constantVars.clear();
        return null;
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        st.setCurrentMethod("main");
        this.constantVars = st.getCurrentMethodScope().getConstantVars();
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        this.constantVars.clear();
        return null;
    }

    private String dealWithVarDcl(JmmNode jmmNode, String s) {
        String varName = jmmNode.get("var");

        //  If the variable is not a constant, then it is safe to remove it
        if (constantVars.contains(varName)) removeNode(jmmNode);
        return null;
    }

    private String dealWithAssign(JmmNode jmmNode, String s) {

        String varName = jmmNode.get("var");

        visit(jmmNode.getJmmChild(0), s); // Visit the expression no matter what

        //  If the variable is not a constant, then it is safe to remove it
        if (constantVars.contains(varName)) removeNode(jmmNode);
        return null;
    }


}