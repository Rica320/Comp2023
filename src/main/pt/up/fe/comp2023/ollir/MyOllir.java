package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.OptimizeVisitors.ConstantFolding;
import pt.up.fe.comp2023.OptimizeVisitors.ConstantPropagation;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.Collections;

public class MyOllir implements JmmOptimization {

    private final int registerNum;
    private final boolean isDebug;

    private final boolean isOptimize;

    public MyOllir(boolean isDebug, int registerNum, boolean isOptimize) {
        this.isDebug = isDebug;
        this.registerNum = registerNum;
        this.isOptimize = isOptimize;
    }

    public MyOllir() {
        this(false, 0, false);
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult analyserResult) {

        if (!isOptimize) return analyserResult;

        MySymbolTable symbolTable = (MySymbolTable) analyserResult.getSymbolTable();
        ConstantFolding constantFolding = new ConstantFolding();
        ConstantPropagation constantPropagation = new ConstantPropagation(symbolTable);

        do {
            analyserResult = constantFolding.optimize(analyserResult);
            if (isDebug) System.out.println(analyserResult.getRootNode().toTree());
            analyserResult = constantPropagation.optimize(analyserResult);
        } while (constantFolding.isChanged() || constantPropagation.isChanged());

        return analyserResult;
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
