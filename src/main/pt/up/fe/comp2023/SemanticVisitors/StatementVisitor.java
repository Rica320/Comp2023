package pt.up.fe.comp2023.SemanticVisitors;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
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

        addVisit("Scope", this::dealWithScope);
        addVisit("IfClause", this::dealWithIfClause);
        //addVisit("While", this::dealWithWhile);
        addVisit("ExpressionStmt", this::dealWithExpressionStmt);
        //addVisit("Assign", this::dealWithAssign);
        //addVisit("ArrayAssign", this::dealWithArrayAssign);
        //setDefaultVisit(this::defaultVisit);
    }

    private Type dealWithScope(JmmNode jmmNode, String s){
        for(JmmNode child : jmmNode.getChildren()){
            visit(child, "");
        }
        return null;
    }

    private Type dealWithIfClause(JmmNode jmmNode, String s){
        return null;
    }

    private Type dealWithExpressionStmt(JmmNode jmmNode, String s){
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        expressionVisitor.visit(jmmNode.getJmmChild(0), "");
        return null;
    }
}
