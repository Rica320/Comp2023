package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.ArrayList;

public class MyJmmAnalysis implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {


        MySymbolTable st = new MySymbolTable(); // N é necessário o nome da classe e a superclasse, correto ?
        st = st.populateSymbolTable(jmmParserResult);

        /// TODO: OS IMPORTS N ESTAO A DAR PARA O TIPO: import java.lang.System; por exemplo
        return new JmmSemanticsResult(jmmParserResult, st, new ArrayList<>());
    }
}
