package pt.up.fe.comp2023.SemanticVisitors;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

public class AnnotateVisitor extends AJmmVisitor<String, String> {

    MySymbolTable st;

    public AnnotateVisitor(MySymbolTable st) {
        this.st = st;
    }

    @Override
    protected void buildVisitor() {

        // Class
        addVisit("MainMethod", this::dealWithMain);
        addVisit("MethodDecl", this::dealWithMethod);

        // Methods
        //addVisit("MethodArgs", this::dealWithMethodArgs);
        // addVisit("ParamDecl", this::dealWithParamDecl);
        // addVisit("MethodCall", this::dealWithMethodCall);

        // addVisit("Int", this::dealWithInt);
        // addVisit("Var", this::dealWithVar);
        // addVisit("Boolean", this::dealWithBool);

        // Statements
        addVisit("Assign", this::dealWithAssign);
        addVisit("IfClause", this::dealWithBool);
        addVisit("While", this::dealWithBool);
        addVisit("ArrayAssign", this::dealWithArrayAssign);


        // Expression
        addVisit("NewIntArray", this::dealWithNewIntArray);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("BinaryComp", this::dealWithBool);
        addVisit("BinaryBool", this::dealWithBool);
        addVisit("AttributeAccess", this::dealWithAtributeAccess);
        addVisit("Not", this::dealWithBool);
        addVisit("ArrayLookup", this::dealWithArrayLookup);

        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithArrayLookup(JmmNode jmmNode, String s) {
        jmmNode.getJmmChild(0).put("expType", "int[]");
        jmmNode.getJmmChild(1).put("expType", "int");
        defaultVisit(jmmNode, s);
        return null;
    }

    private String dealWithAtributeAccess(JmmNode jmmNode, String s) {
        jmmNode.put("expType", "int[]");
        defaultVisit(jmmNode, s);
        return null;
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        jmmNode.put("expType", "int");
        defaultVisit(jmmNode, s);
        return null;
    }

    private String dealWithNewIntArray(JmmNode jmmNode, String s) {
        jmmNode.getJmmChild(0).put("expType", "int");
        defaultVisit(jmmNode, s);
        return null;
    }

    private String dealWithBool(JmmNode jmmNode, String s) {
        jmmNode.put("expType", "boolean");
        defaultVisit(jmmNode, s);

        return null;
    }


    private String dealWithArrayAssign(JmmNode jmmNode, String s) {
        jmmNode.getJmmChild(0).put("expType", "int");
        jmmNode.getJmmChild(1).put("expType", st.findTypeVar(jmmNode.get("name")).getName());
        defaultVisit(jmmNode, s);

        return null;
    }

    private String dealWithAssign(JmmNode jmmNode, String s) {
        jmmNode.put("expType", st.findTypeVar(jmmNode.get("var")).getName());
        defaultVisit(jmmNode, s);

        return null;
    }

    private void propagateDown(JmmNode jmmNode) {
        if (jmmNode.hasAttribute("expType")) {
            for (JmmNode child : jmmNode.getChildren()) {
                child.put("expType", jmmNode.get("expType"));
            }
        }
    }

    private String defaultVisit(JmmNode jmmNode, String s) {
        propagateDown(jmmNode);

        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, "");
        }

        return null;
    }

    private String dealWithMethod(JmmNode jmmNode, String s) {
        st.setCurrentMethod(jmmNode.get("name"));
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        return null;
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        st.setCurrentMethod("main");
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        return null;
    }


}
