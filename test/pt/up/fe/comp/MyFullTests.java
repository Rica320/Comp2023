package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.Jasmin.MyJasminBackend;
import pt.up.fe.comp2023.JmmSemanticAnalyser;
import pt.up.fe.comp2023.SimpleParser;
import pt.up.fe.comp2023.ollir.MyOllir;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsStrings;
import utils.ProjectTestUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class MyFullTests {

    private void test(String resource, String expectedOutput) {
        var code = SpecsIo.getResource("pt/up/fe/comp/MyFullTestFiles/" + resource);
        var parserResult = TestUtils.parse(code);
        var semanticsResult = TestUtils.analyse(parserResult);
        var ollirResult = TestUtils.optimize(semanticsResult);
        var jasminResult = TestUtils.backend(ollirResult);
        ProjectTestUtils.runJasmin(jasminResult, expectedOutput);
    }

    @Test
    public void testHelloWorld() {
        test( "HelloWorld.jmm", "Hello, World!");
    }

    @Test
    public void testFields() {
        test("fields.jmm", "5");
    }

    @Test
    public void testIf() {
        test("if.jmm", "4\n9");
    }

    @Test
    public void testWhile() {
        test("while.jmm", "0\n1\n2\n3\n4\n5\n6\n7\n8\n9");
    }

    @Test
    public void testBasicFuncs() {
        test("basicfuncs.jmm", "0\n1\n14\n35\n-21\n5\n4\n0\n19\n56\n-56");
    }

    @Test
    public void testManyAssign() {
        test("manyassign.jmm", "10");
    }

    @Test
    public void testArrayStuff() {
        test("array1.jmm", "3");
    }

    @Test
    public void testNewObj() {
        test("newobj.jmm", ""); // TODO
    }

    @Test
    public void testHard1() {
        test("hard1.jmm", "");  // TODO
    }

    @Test
    public void testReturnObj() {
        test("returnobj.jmm", "");  // TODO
    }

    @Test
    public void testPrintArray() {
        test("printarr.jmm", "1\n2\n3\n4\n5");
    }

    @Test
    public void testBasicPrints() {
        test("basicprints.jmm", "3\n20\n7\n12\n4");
    }

    @Test
    public void testMultArray() {
        test("multarr.jmm", "0\n6\n12\n18\n24\n20\n24\n28\n32\n36");
    }

    @Test
    public void testIfHell() {
        test("ifhell.jmm", "3\n5\n99");
    }

    @Test
    public void testCallHell() {
        test("callhell.jmm", "75");
    }

}
