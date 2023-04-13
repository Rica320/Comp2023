package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.SemanticVisitors.ExpressionVisitor;
import pt.up.fe.comp2023.SemanticVisitors.ProgramVisitor;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.ArrayList;
import java.util.List;


// todo: add semantic analysis
// Return type checking
// argument number and type checking on method calls
// Check if theres a main method and if the only argument is String[] args

// attribute = length checking
// var assign must be of the same type as declared
// Check if [INDEX] is only used on arrays and if index given is int

// Binary op must have same type on both sides
// If/While must have boolean condition


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
            Report report = new Report(ReportType.DEBUG, Stage.SEMANTIC, -1, message);
            reports.add(report);
        }

        ProgramVisitor arrayOperationVisitor = new ProgramVisitor(st, reports);
        arrayOperationVisitor.visit(jmmParserResult.getRootNode());
        //List<Report> temp = arrayOperationVisitor.getReports();
        //reports.addAll(temp);


        // ... more semantic analysis
        return new JmmSemanticsResult(jmmParserResult, st, reports);
    }
}
