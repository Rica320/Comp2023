package pt.up.fe.comp2023.OptimizeVisitors;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConstantPropagation extends AJmmVisitor<String, String> {


    private final HashMap<String, Pair<String, Integer>> variables = new HashMap<>(); // Variable name -> value
    MySymbolTable st;
    private List<String> constantVars = new ArrayList<>(); // Variables that are constants
    private boolean changed = false;

    public ConstantPropagation(MySymbolTable st) {
        this.st = st;
    }

    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        changed = false;
        JmmNode root = semanticsResult.getRootNode();

        PropagationVisitorUpdater visitorUpdater = new PropagationVisitorUpdater(this.st);
        visitorUpdater.visit(root, "");

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
        this.constantVars = new ArrayList<>();
        variables.clear();
        return null;
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        st.setCurrentMethod("main");
        this.constantVars = st.getCurrentMethodScope().getConstantVars();
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        this.constantVars = new ArrayList<>();
        variables.clear();
        return null;
    }


    private String dealWithAssign(JmmNode jmmNode, String s) {

        String varName = jmmNode.get("var");

        // We have to visit the right side no matter what because
        // it may contain an expression that has a const var in it to be replaced
        visit(jmmNode.getJmmChild(0), s);

        if (!constantVars.contains(varName)) return null;

        // if (variables.containsKey(varName)) THROW ERROR  // This never happens ?
        // If the variable is being assigned a constant, add it to the variables map
        String kind = jmmNode.getJmmChild(0).getKind();
        if (kind.equals("Int") || kind.equals("Boolean"))
            variables.put(varName, new Pair<>(kind, Integer.parseInt(jmmNode.getJmmChild(0).get("val"))));
        return null;
    }


    private String dealWithVar(JmmNode jmmNode, String s) {
        // If the variable is being used, replace it with its value
        String varName = jmmNode.get("var");
        if (variables.containsKey(varName)) {
            Pair<String, Integer> pair = variables.get(varName); // type, value
            JmmNode newNode = new JmmNodeImpl(pair.a); // Create a new node with the same type
            newNode.put("val", pair.b.toString()); // Set the value
            replaceNode(jmmNode, newNode);
        }
        return null;
    }

}
