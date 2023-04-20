package pt.up.fe.comp2023.SemanticVisitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.List;

public class AnnotateVisitor extends AJmmVisitor<String, String> {

    private final List<Report> reports;
    MySymbolTable st;

    public AnnotateVisitor(MySymbolTable st, List<Report> reports) {

        this.st = st;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {

        // Class
        addVisit("MainMethod", this::dealWithMain);
        addVisit("MethodDecl", this::dealWithMethod);

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
        jmmNode.getJmmChild(0).put("expType", "boolean");
        defaultVisit(jmmNode, s);

        return null;
    }


    private String dealWithArrayAssign(JmmNode jmmNode, String s) {
        jmmNode.getJmmChild(0).put("expType", "int");
        try {
            jmmNode.getJmmChild(1).put("expType", st.findTypeVar(jmmNode.get("var")).getName());
        } catch (Exception e) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")), "Array is not declared"));
        }
        defaultVisit(jmmNode, s);

        return null;
    }

    private String dealWithAssign(JmmNode jmmNode, String s) {
        var varAux = st.findTypeVar(jmmNode.get("var"));
        if (varAux == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")), "Var not declared"));
            return null;
        }

        jmmNode.put("expType", varAux.getName());
        System.out.println("Assign: " + jmmNode.get("var") + " " + jmmNode.get("expType"));
        defaultVisit(jmmNode, s);

        return null;
    }

    private void propagateDown(JmmNode jmmNode) {
        if (jmmNode.hasAttribute("expType")) {
            for (JmmNode child : jmmNode.getChildren()) {
                System.out.println("Propagate: " + child.getKind() + " " + jmmNode.get("expType"));
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
