package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.Jasmin.MyJasminBackend;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class MyJasminTests {

    String fileJasmin = "pt/up/fe/comp/jasmin/MyInputJasmin.j";
    String fileOllir = "pt/up/fe/comp/jasmin/MyInputOllir.ollir";

    @Test
    public void runOllirToJasmin() {
        String ollirCode = SpecsIo.getResource(fileOllir);
        OllirResult ollirResult = new OllirResult(ollirCode, new HashMap<>());
        MySymbolTable st = new MySymbolTable(null);
        var jasminResult = new MyJasminBackend(st).toJasmin(ollirResult);
        var jasminCode = jasminResult.getJasminCode();
        var output = TestUtils.runJasmin(jasminCode);
        //assertEquals("120", SpecsStrings.normalizeFileContents(output).trim());
    }

    @Test
    public void runJasminCode() {
        String jasminCode = SpecsIo.getResource(fileJasmin);
        var output = TestUtils.runJasmin(jasminCode);
    }


}
