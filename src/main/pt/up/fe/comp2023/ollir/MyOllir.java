package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class MyOllir implements JmmOptimization {

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        System.out.println("Optimizing semantics result : n = " + semanticsResult.getConfig().get("registerAllocation"));
        return semanticsResult;
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {

        SymbolTable symbolTable = jmmSemanticsResult.getSymbolTable();
        MyOllirVisitor myOllirVisitor = new MyOllirVisitor(symbolTable);

        String code = myOllirVisitor.visit(jmmSemanticsResult.getRootNode()).a;

        System.out.println(code);
        return new OllirResult(jmmSemanticsResult, code, Collections.emptyList());
        //return null;
    }


    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        System.out.println("Optimizing OLLIR code");
        return ollirResult;
    }

}
