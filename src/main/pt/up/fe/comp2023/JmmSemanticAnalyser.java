package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.ArrayList;
import java.util.List;

public class JmmSemanticAnalyser implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {

        // Create Symbol Table from Parser Result and print it
        MySymbolTable st = new MySymbolTable(jmmParserResult);
        System.out.println(st);

        // Semantic Analysis
        List<Report> reports = new ArrayList<>();

        // Overload Analysis
        List<List<String>> overloads = st.getOverloads();
        for (List<String> overload : overloads) {
            String message = "Overload found in " + overload.get(0) + " -> " + overload.get(1) + " '" + overload.get(2) + "'";
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message);
            reports.add(report);
        }

        // ... more semantic analysis
        return new JmmSemanticsResult(jmmParserResult, st, reports);
    }
}
