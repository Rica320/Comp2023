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
    private List<Report> reports;

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

    private Type dealWithArrayAssign(JmmNode jmmNode, String s) {
        jmmNode.getJmmChild(0).put("expType", jmmNode.get("var"));

        return null;
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
        return null;
    }

    private Type dealWithAssign(JmmNode jmmNode, String s){
        Type visitResult;
        for (int i = 0; i < jmmNode.getNumChildren(); ++i) {
            JmmNode childNode = jmmNode.getJmmChild(i);
            visitResult = visit(childNode);
            if (visitResult.getName().equals("error")) {
                return visitResult;
            }
        }
        jmmNode.getJmmChild(0).put("expType", jmmNode.get("var"));




        if (jmmNode.getNumChildren() == 2) {
            JmmNode leftChild = jmmNode.getJmmChild(0);
            Type leftType = visit(leftChild, "");

            JmmNode rightChild = jmmNode.getJmmChild(1);
            Type rightType = visit(rightChild, "");

            //check if its a variable
            //jmmNode.getKind().equals(AstTypes.IDENTIFIER.toString()) || jmmNode.getKind().equals(AstTypes.ARRAY_EXPR.toString());

            /*if (!this.isVariable(leftChild)) {
                this.reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(leftChild.get("lineStart")), Integer.parseInt(leftChild.get("colStart")),
                        "Attempting to assign a value to a '" + leftChild.getKind() + "' .",
                        null));
                return new Type("error", false);
            } else if (!leftType.equals(rightType)) {
                this.reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(leftChild.get("lineStart")), Integer.parseInt(leftChild.get("colStart")),
                        "Attempting to assign value of type " + rightType + " to a variable of type " + leftType, null));
                return new Type("error", false);
            }*/

            rightChild.put("type", rightType.getName());

            return rightType;
        }

        throw new RuntimeException("Illegal number of children in node " + "." + jmmNode.getKind());

    }
}
