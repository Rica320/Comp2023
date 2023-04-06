package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.Jasmin.MyJasminBackend;
import pt.up.fe.specs.util.SpecsIo;

import java.util.HashMap;

public class MyJasminTests {

    String fileJasmin = "pt/up/fe/comp/jasmin/MyInputJasmin.j";
    String fileOllir = "pt/up/fe/comp/jasmin/MyInputOllir.ollir";

    @Test
    public void runOllirToJasmin() {
        String ollirCode = SpecsIo.getResource(fileOllir);
        OllirResult ollirResult = new OllirResult(ollirCode, new HashMap<>());
        var jasminResult = new MyJasminBackend().toJasmin(ollirResult);
        var jasminCode = jasminResult.getJasminCode();

        try {
            var output = TestUtils.runJasmin(jasminCode);
        } catch (Exception e) {
            System.out.println("Count not run Jasmin code: " + e.getMessage());
        }

        //assertEquals("120", SpecsStrings.normalizeFileContents(output).trim());
    }




}
