package pt.up.fe.comp2023.OptimizeVisitors;

import org.antlr.v4.runtime.misc.Triple;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.HashMap;

public class ConstantPropagation2 extends AJmmVisitor<String, String> {

    MySymbolTable st;
    private HashMap<String, Triple<String, Integer, Integer>> vars = new HashMap<>();  // varName : varKind, varScope, varValue
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
        addVisit("IfClause", this::dealWithIfClause);
        addVisit("While", this::dealWithWhile);
        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithWhile(JmmNode jmmNode, String s) {
        // 'while' '(' expression ')' while_block #While
        visit(jmmNode.getJmmChild(0), s);

        scope++;
        visit(jmmNode.getJmmChild(1), s);
        scope--;

        var whileScope = jmmNode.getJmmChild(1);

        // remove tainted variables
        for (JmmNode child : whileScope.getJmmChild(0).getChildren())
            if (child.getKind().equals("Assign")) vars.remove(child.get("var"));

        return null;
    }

    private String dealWithIfClause(JmmNode jmmNode, String s) {

        visit(jmmNode.getJmmChild(0), s);

        var vars_copy = new HashMap<>(vars);

        var ifScope = jmmNode.getJmmChild(1);
        scope++;
        visit(ifScope, s);
        scope--;

        vars = vars_copy;

        var elseScope = jmmNode.getJmmChild(2);
        scope++;
        visit(elseScope, s);
        scope--;

        // remove tainted variables
        for (JmmNode child : ifScope.getJmmChild(0).getChildren())
            if (child.getKind().equals("Assign")) vars.remove(child.get("var"));

        for (JmmNode child : elseScope.getJmmChild(0).getChildren())
            if (child.getKind().equals("Assign")) vars.remove(child.get("var"));

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
        st.setCurrentMethod(null);
        return null;
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        st.setCurrentMethod("main");
        scope = 0;
        defaultVisit(jmmNode, s);
        vars.clear();
        st.setCurrentMethod(null);
        return null;
    }

    private String dealWithVar(JmmNode jmmNode, String s) {

        // check if variable is in the list
        String varname = jmmNode.get("var");

        if (vars.containsKey(varname)) {
            Triple<String, Integer, Integer> var = vars.get(varname);
            JmmNode newNode = new JmmNodeImpl(var.a, jmmNode);
            newNode.put("val", var.c.toString());
            replaceNode(jmmNode, newNode);
        }

        return null;
    }

    private String dealWithAssign(JmmNode jmmNode, String s) {

        // Add all variables assigned to a constant to the list
        String kind = jmmNode.getJmmChild(0).getKind();
        String varname = jmmNode.get("var");

        visit(jmmNode.getJmmChild(0), s);

        if (kind.equals("Int") || kind.equals("Boolean")) {
            int value = Integer.parseInt(jmmNode.getJmmChild(0).get("val"));
            vars.put(varname, new Triple<>(kind, scope, value));
        } else vars.remove(varname);


        return null;
    }

}
