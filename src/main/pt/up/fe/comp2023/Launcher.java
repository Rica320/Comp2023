package pt.up.fe.comp2023;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp2023.Jasmin.MyJasminBackend;
import pt.up.fe.comp2023.OptimizeVisitors.ConstantFolding;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;
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

        System.out.println("Config: " + config);

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

        ConstantFolding constantFolding = new ConstantFolding((MySymbolTable) analyserResult.getSymbolTable());
        analyserResult = constantFolding.optimize(analyserResult);

        MyOllir myOllir = new MyOllir();

/*        if (!config.get("registerAllocation").equals("-1")) {
            analyserResult = myOllir.optimize(analyserResult);
        }*/

        OllirResult ollirResult = myOllir.toOllir(analyserResult);


/*        if (config.get("optimize").equals("true")) {
            ollirResult = myOllir.optimize(ollirResult);
        }*/

        JasminResult jasminResult = new MyJasminBackend().toJasmin(ollirResult);
        String jasminCode = jasminResult.getJasminCode();

        System.out.println(jasminCode);
        TestUtils.runJasmin(jasminCode);
//
        // try {
        //     String output = TestUtils.runJasmin(jasminCode);
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

        // Instantiate JmmCodeGenerator
        // SimpleCodeGenerator codeGenerator = new SimpleCodeGenerator();


    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
/*        if (args.length <= 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }*/

        List<String> options = Arrays.stream(args).toList();

        String registerAllocation = options.stream()
                .filter(option -> option.startsWith("-r"))
                .findFirst()
                .orElse("-1");
        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", options.contains("-o") ? "true" : "false");
        //config.put("registerAllocation", registerAllocation.strip().split("\\=")[1]);
        //System.out.println("Register allocation: " + registerAllocation.split("\\=")[1]);
        //System.out.println("Optimize: " + config.get("optimize"));
        config.put("debug", "false");

        return config;
    }

}
