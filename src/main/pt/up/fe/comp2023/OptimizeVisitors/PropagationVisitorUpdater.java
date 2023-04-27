package pt.up.fe.comp2023.OptimizeVisitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

public class PropagationVisitorUpdater extends AJmmVisitor<String, String> {

    MySymbolTable st;

    public PropagationVisitorUpdater(MySymbolTable st) {
        this.st = st;
    }

    @Override
    protected void buildVisitor() {

        // Class
        addVisit("MainMethod", this::dealWithMain);
        addVisit("MethodDecl", this::dealWithMethod);

        // Statements
        addVisit("Assign", this::dealWithAssign);

        setDefaultVisit(this::defaultVisit);
    }

    private String defaultVisit(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren())
            visit(child, "");
        return null;
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        st.setCurrentMethod("main");
        st.clearConstantVars();
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        return null;
    }

    private String dealWithMethod(JmmNode jmmNode, String s) {
        st.setCurrentMethod(jmmNode.get("name"));
        st.clearConstantVars();
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        return null;
    }

    private String dealWithAssign(JmmNode jmmNode, String s) {
        var varAux = st.findTypeVar(jmmNode.get("var"));

        // If the variable is being assigned a constant, add it to the variables map
        String kind = jmmNode.getJmmChild(0).getKind();
        if (kind.equals("Int") || kind.equals("Boolean")){
            st.addConstantVar(jmmNode.get("var"));
            System.out.println("Added constant var: " + jmmNode.get("var") + " in " + st.getCurrentMethod() + " " + jmmNode);
        }else{
            // impede que variavel nao associada a constante possa ser usada como constante
            st.addConstantVar(jmmNode.get("var"));
            st.addConstantVar(jmmNode.get("var"));
        }

        defaultVisit(jmmNode, s);
        return null;
    }
}
