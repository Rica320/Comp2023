package pt.up.fe.comp2023;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;
import pt.up.fe.comp2023.Visitor.SymbolTableVisitor;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);

        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }

        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, config);

        if (parserResult.getReports().size() > 0) {
            for (Report report : parserResult.getReports()) {
                if (report.getType() == ReportType.ERROR || report.getType() == ReportType.WARNING) {
                    System.out.println("Error: " + report.getMessage());
                    System.out.println("Line: " + report.getLine());
                    System.out.println("Column: " + report.getColumn());
                    System.out.println("Stage: " + report.getStage());
                    System.out.println("Type: " + report.getType());
                }
            }
        } else if (parserResult.getRootNode() == null) {
            System.out.println("Parser result is null!");
            return;
        } else {
            System.out.println("No errors found!\n\n");
        }

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // Print full AST
        System.out.println(parserResult.getRootNode().toTree());

        // Instantiate JmmAnalyser
        JmmSemanticAnalyser analyser = new JmmSemanticAnalyser();

        // Analyse stage
        JmmSemanticsResult analyserResult = analyser.semanticAnalysis(parserResult);

        // Check if there are semantic errors
        TestUtils.noErrors(analyserResult.getReports());

        // Instantiate JmmCodeGenerator
        // SimpleCodeGenerator codeGenerator = new SimpleCodeGenerator();

        // Generate stage
        // SimpleCodeGeneratorResult codeGeneratorResult = codeGenerator.generate(analyserResult, config);

        // Check if there are code generation errors
        // TestUtils.noErrors(codeGeneratorResult.getReports());

        // Print generated code
        // System.out.println(codeGeneratorResult.getCode());

        // ... add remaining stages
    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        return config;
    }

}
