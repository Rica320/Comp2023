package pt.up.fe.comp2023.SemanticVisitors;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;


import java.util.List;

public class ProgramVisitor extends AJmmVisitor<String, Type> {

    private final MySymbolTable st;
    private final List<Report> reports;

    public ProgramVisitor(MySymbolTable table, List<Report> reports){
        this.st = table;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        addVisit("ProgramRoot", this::dealWithProgram);
        addVisit("ClassDecl", this::dealWithClass);
        addVisit("MainMethod", this::dealWithMethodDecl);
        addVisit("MethodDecl", this::dealWithMethodDecl);
        addVisit("ReturnStmt", this::dealWithReturn);

        setDefaultVisit(this::defaultVisit);
    }


    private Type defaultVisit(JmmNode jmmNode, String s) {
        for(JmmNode node: jmmNode.getChildren()) {
            visit(node);
        }
        return null;
    }

    private Type dealWithProgram(JmmNode jmmNode, String s){
        for(JmmNode child : jmmNode.getChildren()){
            if(child.getKind().equals("ClassDecl")) {
                visit(child, "");
            }
        }
        return null;
    }

    private Type dealWithClass(JmmNode jmmNode, String s) {
        for(JmmNode child: jmmNode.getChildren()){
            if(child.getKind().equals("Method")){
                visit(child, "");
            }
        }
        return null;
    }


    private Type dealWithMethodDecl(JmmNode jmmNode, String s) {
        for(JmmNode child: jmmNode.getChildren()){
            switch (child.getKind()) {
                case "Scope", "ExpressionStmt", "IfClause", "While", "Assign", "ArrayAssign" -> {
                    StatementVisitor statementVisitor = new StatementVisitor(st, reports);
                    Type childType = statementVisitor.visit(child, "");
                }
                case "ReturnStmt" -> visit(child, "");
            }
        }
        return null;
    }

    private Type dealWithReturn(JmmNode jmmNode, String s) {

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);

        Type retType = expressionVisitor.visit(jmmNode.getJmmChild(0));

        if(!retType.equals(st.getCurrentMethodScope().getReturnType())){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Return type does not match method return type"));
        }

        return new Type("null", false);
    }


}
