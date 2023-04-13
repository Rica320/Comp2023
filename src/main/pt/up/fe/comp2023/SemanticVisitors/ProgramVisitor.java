package pt.up.fe.comp2023.SemanticVisitors;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
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
        //addVisit("varDcl", this::dealWithVarDecl); ???
        //addVisit("MainMethod", this::dealWithMethodDecl);
        //addVisit("MethodDecl", this::dealWithMethodDecl);
        //addVisit("MethodArgs", this::dealWithMethodArgs);
        //addVisit("ParamDecl", this::dealWithParamDecl);
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


}
