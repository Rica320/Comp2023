package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.OptimizeVisitors.ConstantFolding;
import pt.up.fe.comp2023.OptimizeVisitors.ConstantPropagation;
import pt.up.fe.comp2023.OptimizeVisitors.registerAllocation.RegisterAllocation;
import pt.up.fe.comp2023.OptimizeVisitors.RemoveUnusedVars;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.Collections;

public class MyOllir implements JmmOptimization {

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult analyserResult) {

        var config = analyserResult.getConfig();
        boolean isDebug = config.getOrDefault("debug", "false").equals("true");
        boolean isOptimize = config.getOrDefault("optimize", "false").equals("true");

        if (!isOptimize) return analyserResult;

        MySymbolTable symbolTable = (MySymbolTable) analyserResult.getSymbolTable();
        ConstantFolding constantFolding = new ConstantFolding();
        ConstantPropagation constantPropagation = new ConstantPropagation(symbolTable);
        RemoveUnusedVars removeUnusedVars = new RemoveUnusedVars(symbolTable);

        do {
            analyserResult = constantFolding.optimize(analyserResult);
            if (isDebug) System.out.println(analyserResult.getRootNode().toTree());
            analyserResult = constantPropagation.optimize(analyserResult);
            if (isDebug) System.out.println(analyserResult.getRootNode().toTree());
           // analyserResult = removeUnusedVars.optimize(analyserResult);
            if (isDebug) System.out.println(analyserResult.getRootNode().toTree());
        } while (constantFolding.isChanged() || constantPropagation.isChanged() || removeUnusedVars.isChanged());

        return analyserResult;
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {

        SymbolTable symbolTable = jmmSemanticsResult.getSymbolTable();


        MyOllirVisitor myOllirVisitor = new MyOllirVisitor(symbolTable);

        String code = myOllirVisitor.visit(jmmSemanticsResult.getRootNode()).a;

        return new OllirResult(jmmSemanticsResult, code, Collections.emptyList());
    }


    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        System.out.println("Optimizing OLLIR code...");

        System.out.println(ollirResult.getOllirClass().getMethods().get(0).getInstructions());


        int regNumAlloc = Integer.parseInt(ollirResult.getConfig()
                .getOrDefault("registerAllocation", "-1"));

        if (regNumAlloc >= 0) {
            RegisterAllocation registerAllocation = new RegisterAllocation(ollirResult.getOllirClass(), regNumAlloc);
            registerAllocation.run();
        }

        return ollirResult;
    }

}
