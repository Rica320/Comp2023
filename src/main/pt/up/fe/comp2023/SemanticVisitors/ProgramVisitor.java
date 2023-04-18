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
        addVisit("MainMethod", this::dealWithMainMethod);
        addVisit("MethodDecl", this::dealWithMethodDecl);
        addVisit("ReturnStmt", this::dealWithReturn);
    }

    private Type dealWithMainMethod(JmmNode jmmNode, String s) {
        st.setCurrentMethod("main");
        return visitMethodChildren(jmmNode);
    }

    private Type visitMethodChildren(JmmNode jmmNode) {
        for(JmmNode child: jmmNode.getChildren()){
            switch (child.getKind()) {
                case "Scope", "ExpressionStmt", "IfClause", "While", "Assign", "ArrayAssign" -> {
                    StatementVisitor statementVisitor = new StatementVisitor(st, reports);
                    Type childType = statementVisitor.visit(child, "");
                }
                case "ReturnStmt" -> visit(child, "");
            }
        }
        st.setCurrentMethod(null);
        return null;
    }


    /*private Type defaultVisit(JmmNode jmmNode, String s) {
        for(JmmNode node: jmmNode.getChildren()) {
            visit(node);
        }
        return null;
    }*/

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
            if(child.getKind().equals("MethodDecl") || child.getKind().equals("MainMethod")){
                visit(child, "");
            }
        }
        return null;
    }
/*
for(int i = 0; i < children.size(); i++){
            JmmNode child = children.get(i);
            Type childType = new Type("",false);
            switch (child.getKind()) {
                case "Scope", "ExpressionStmt", "IfClause", "While", "Assign", "ArrayAssign" -> {
                    StatementVisitor statementVisitor = new StatementVisitor(st, reports);
                    childType = statementVisitor.visit(child, "");
                }
                case "ReturnStmt" -> visit(child, "");
                case "Paren", "ArrayLookup", "AttributeAccess", "MethodCall", "Not", "NewIntArray", "NewObject", "BinaryOp", "BinaryComp","BinaryBool", "This", "Var", "Boolean", "Int" -> {
                    ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
                    childType = expressionVisitor.visit(child, "");
                }
            }
        }
 */

    private Type dealWithMethodDecl(JmmNode jmmNode, String s) {
        System.out.println("method name: " + jmmNode.get("name"));
        st.setCurrentMethod(jmmNode.get("name"));
        return visitMethodChildren(jmmNode);
    }

    private Type dealWithReturn(JmmNode jmmNode, String s) {

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);

        System.out.println("return type: " + st.getCurrentMethodScope().getReturnType().getName());

        Type retType = expressionVisitor.visit(jmmNode.getJmmChild(0));

        if(!retType.getName().equals("importCorrect")
            && !retType.equals(st.getCurrentMethodScope().getReturnType())) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Return type does not match method return type"
                            + "\nreturn type: " +
                            st.getCurrentMethodScope().getReturnType().getName()
                            + "\nmethod return type: " + retType.getName()
                            + "\nmethod name: " + st.getCurrentMethod()));
        }
        return new Type("null", false);
    }


}
