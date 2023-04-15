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
//Array access index is an expression of type integer

public class ProgramVisitor extends AJmmVisitor<String, Type> {

    private MySymbolTable st;
    private List<Report> reports;

    public ProgramVisitor(MySymbolTable table, List<Report> reports){
        this.st = table;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        addVisit("ProgramRoot", this::dealWithProgram);
        addVisit("ImportDecl", this::dealWithImport);
        addVisit("ClassDecl", this::dealWithClass);
        addVisit("VarDcl", this::dealWithVarDecl);
        addVisit("MainMethod", this::dealWithMethodDecl);
        addVisit("MethodDecl", this::dealWithMethodDecl);
        //addVisit("ReturnStmt", this::dealWithReturn);

        //defaultVisit(this::defaultVisit);
    }

    //private String defaultVisit(JmmNode jmmNode, String s) {return "DEFAULT_VISIT";}

    private Type dealWithProgram(JmmNode jmmNode, String s){
        for(JmmNode child : jmmNode.getChildren()){
            if(child.getKind().equals("ClassDecl")) {
                visit(child, "");
            }
        }
        return new Type("null", false);
    }

    private Type dealWithImport(JmmNode jmmNode, String s){
        return new Type("null", false);
    }

    private Type dealWithClass(JmmNode jmmNode, String s) {
        for(JmmNode child: jmmNode.getChildren()){
            if(child.getKind().equals("Method")){
                visit(child, "");
            }
        }
        return new Type("null", false);
    }

    private Type dealWithVarDecl(JmmNode jmmNode, String s) {
        //System.out.println("VarDeclaration:" + jmmNode.get("var"));
        visit(jmmNode.getJmmChild(0), "");
        return new Type("null", false);
    }

    private Type dealWithMethodDecl(JmmNode jmmNode, String s) {
        for(JmmNode child: jmmNode.getChildren()){
            switch (child.getKind()) {
                case "Scope", "ExpressionStmt", "IfClause", "While", "Assign", "ArrayAssign":
                    StatementVisitor statementVisitor = new StatementVisitor(st, reports);
                    Type childType = statementVisitor.visit(child, "");
                    break;
                case "ReturnStmt":
                    visit(child, "");
                    break;
            }
        }
        return new Type("null", false);
    }


    /*private Type dealWithReturn(JmmNode jmmNode, String s) {
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        Type childType = expressionVisitor.visit(jmmNode.getJmmChild(0), "");
        JmmNode expression = jmmNode.getJmmChild(0);
        Type exprType = visit(expression, "");
        if(!exprType.equals(st.getReturnType())){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Return type does not match method return type"));
        }
        //se não for igual ver se o 1 pertence a um import, se sim aceitar
        return new Type("null", false);
    }*/


}
