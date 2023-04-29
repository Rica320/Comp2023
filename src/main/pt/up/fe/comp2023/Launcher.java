package pt.up.fe.comp2023;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp2023.Jasmin.MyJasminBackend;
import pt.up.fe.comp2023.ollir.MyOllir;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);
        System.out.println("Running with config: " + config);

        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile())
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");

        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Load other arguments from config
        boolean isDebug = config.get("debug").equals("true");
        boolean isOptimize = config.get("optimize").equals("true");
        int registerAllocation = Integer.parseInt(config.get("registerAllocation"));

        // ========================= Parsing =========================

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, config);

        // Check if there are parsing errors
        if (checkParsingErrors(parserResult)) return;

        // Print full AST
        if (isDebug) System.out.println(parserResult.getRootNode().toTree());

        // ========================= Semanthics =========================

        // Instantiate JmmAnalyser
        JmmSemanticAnalyser analyser = new JmmSemanticAnalyser();

        // Analyse stage
        JmmSemanticsResult analyserResult = analyser.semanticAnalysis(parserResult);

        // Check if there are semantic errors
        TestUtils.noErrors(analyserResult.getReports());

        // ========================= OLLIR =========================

        // Instantiate MyOllir
        MyOllir myOllir = new MyOllir(isDebug, registerAllocation, isOptimize);

        // Optimize AST before generating OLLIR code
        if (isOptimize) analyserResult = myOllir.optimize(analyserResult);

        // Print full AST after optimization
        if (isDebug) System.out.println(analyserResult.getRootNode().toTree());

        // Generate OLLIR code
        OllirResult ollirResult = myOllir.toOllir(analyserResult);

        // ========================= BACKEND JASMIN =========================

        // Instantiate MyJasminBackend
        MyJasminBackend jasminBackend = new MyJasminBackend(registerAllocation, isDebug);

        // Generate Jasmin code
        JasminResult jasminResult = jasminBackend.toJasmin(ollirResult);

        // Get Jasmin code
        String jasminCode = jasminResult.getJasminCode();

        // Run Code
        TestUtils.runJasmin(jasminCode);
    }

    private static boolean checkParsingErrors(JmmParserResult parserResult) {
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
            return true;
        } else if (parserResult.getRootNode() == null) {
            System.out.println("Parser result is null!");
            return true;
        } else System.out.println("No errors found!\n\n");
        return false;
    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length < 1)
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");

        List<String> options = Arrays.stream(args).toList();

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("debug", options.contains("-d") ? "true" : "false");
        config.put("optimize", options.contains("-o") ? "true" : "false");

        if (options.contains("-r")) config.put("registerAllocation", options.get(options.indexOf("-r") + 2));
        else config.put("registerAllocation", "-1");

        return config;
    }

}
