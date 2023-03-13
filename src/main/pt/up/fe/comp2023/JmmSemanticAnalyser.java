package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.ArrayList;

public class JmmSemanticAnalyser implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {

        // Create Symbol Table from Parser Result and print it
        MySymbolTable st = new MySymbolTable(jmmParserResult);
        System.out.println(st);

        // TO-Do in next delivery: Semantic Analysis
        ArrayList<Report> reports = new ArrayList<>();
        // ...

        return new JmmSemanticsResult(jmmParserResult, st, reports);
    }
}
