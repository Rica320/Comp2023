package pt.up.fe.comp2023.SemanticVisitors;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.List;

//Type verification
//Array access is done over an array
public class StatementVisitor extends AJmmVisitor<String, Type> {
    private final MySymbolTable st;
    private final List<Report> reports;

    public StatementVisitor(MySymbolTable table, List<Report> reports){
        this.st = table;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {

        addVisit("IfClause", this::dealWithConditional);
        addVisit("While", this::dealWithConditional);
        addVisit("ExpressionStmt", this::dealWithExpressionStmt);
        addVisit("Assign", this::dealWithAssign);
        addVisit("ArrayAssign", this::dealWithArrayAssign);
        setDefaultVisit(this::defaultVisit);
    }


    private Type defaultVisit(JmmNode jmmNode, String s){
        for(JmmNode child : jmmNode.getChildren()){
            visit(child, "");
        }
        return new Type("null", false);
    }

    private Type dealWithConditional(JmmNode jmmNode, String s){
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        expressionVisitor.visit(jmmNode.getJmmChild(0), "");
        JmmNode child = jmmNode.getJmmChild(0);
        Type childType = expressionVisitor.visit(child, "");

        if(!childType.getName().equals("boolean")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Conditional expression must be boolean" ));
            return new Type("error", false);
        }

        return childType;
    }

    private Type dealWithExpressionStmt(JmmNode jmmNode, String s){
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        expressionVisitor.visit(jmmNode.getJmmChild(0), "");
        return new Type("null", false);
    }

    private Type dealWithAssign(JmmNode jmmNode, String s){
        Type leftT = st.findTypeVar(jmmNode.get("var"));

        JmmNode right = jmmNode.getJmmChild(0);
        Type rightType = visit(right, "");

        if (!leftT.equals(rightType)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempting to assign two different types"));
            return new Type("error", false);
        }
        return new Type("null", false);
    }

    private Type dealWithArrayAssign(JmmNode jmmNode, String s) {

        boolean isArr = st.findTypeVar(jmmNode.get("var")).isArray();

        if (!isArr) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempting to assign a value to a non-array variable" ));
            return new Type("error", false);
        }

        JmmNode left = jmmNode.getJmmChild(0);
        Type leftType = visit(left);

        if (leftType.getName().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Index must be an int" ));
            return new Type("error", false);
        }

        JmmNode right = jmmNode.getJmmChild(1);
        Type rightType = visit(right, "");

        if (!rightType.getName().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempting to assign a value that is not a int"));
            return new Type("error", false);
        }
        return new Type("null", false);
    }
}
