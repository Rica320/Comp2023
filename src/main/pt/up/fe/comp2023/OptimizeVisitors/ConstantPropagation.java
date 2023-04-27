package pt.up.fe.comp2023.OptimizeVisitors;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConstantPropagation extends AJmmVisitor<String, String> {


    private final HashMap<String, Integer> variables = new HashMap<>(); // Variable name -> value
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
        System.out.println("Propagated constant " + oldNode.get("var") + " to " + newNode.get("val"));
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
        for (JmmNode child : jmmNode.getChildren())
            visit(child, s);
        return "";
    }

    private String dealWithMethod(JmmNode jmmNode, String s) {
        st.setCurrentMethod(jmmNode.get("name"));
        this.constantVars = st.getCurrentMethodScope().getConstantVars();
        System.out.println("Constant vars in " + jmmNode.get("name") + ": " + constantVars);
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        this.constantVars = new ArrayList<>();
        variables.clear();
        return "";
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        st.setCurrentMethod("main");
        this.constantVars = st.getCurrentMethodScope().getConstantVars();
        System.out.println("Constant vars in main: " + constantVars);
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        this.constantVars = new ArrayList<>();
        variables.clear();
        return "";
    }


    private String dealWithAssign(JmmNode jmmNode, String s) {

        String varName = jmmNode.get("var");
        visit(jmmNode.getJmmChild(0), s); // i have to visit the right side no matter what because it can be an expression that has a const var in it to be replaced
        if (!constantVars.contains(varName)) return "";

        if (variables.containsKey(varName))
            throw new RuntimeException("Constant variable '" + varName + "' is being assigned twice.\n" + variables + "\n" + jmmNode + "\n" + st.getCurrentMethod() + "\n" + constantVars);

        // If the variable is being assigned a constant, add it to the variables map
        String kind = jmmNode.getJmmChild(0).getKind();
        if (kind.equals("Int") || kind.equals("Boolean")) {
            variables.put(varName, Integer.parseInt(jmmNode.getJmmChild(0).get("val")));
            System.out.println("Added constant " + varName + " = " + jmmNode.getJmmChild(0).get("val") + " to variables");
        }


        return "";
    }


    private String dealWithVar(JmmNode jmmNode, String s) {
        // If the variable is being used, replace it with its value
        String varName = jmmNode.get("var");
        System.out.println("HERE2! " + varName + " " + variables.get(varName));
        if (variables.containsKey(varName)) {
            JmmNode newNode = new JmmNodeImpl("Int");
            System.out.println("HERE! " + varName + " " + variables.get(varName));
            newNode.put("val", variables.get(varName).toString());
            replaceNode(jmmNode, newNode);
        }
        return "";
    }

}
