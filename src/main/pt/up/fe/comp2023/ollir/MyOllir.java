package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class MyOllir implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {

        SymbolTable symbolTable = jmmSemanticsResult.getSymbolTable();
        MyOllirVisitor myOllirVisitor = new MyOllirVisitor(symbolTable);


        myOllirVisitor.visit(jmmSemanticsResult.getRootNode());

        System.out.println(jmmSemanticsResult.getRootNode().toString());

        return null;
    }

}
