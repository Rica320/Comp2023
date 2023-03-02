package pt.up.fe.comp2023.SymbolTable;


import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class ST_tests {
    public static void main(String[] args) {

        MySymbolTable st = new MySymbolTable("Test", "Super.java");

        System.out.println(st.getClassName());
        System.out.println(st.getSuper());

        st.addField(new MySymbol(new Type("int", false), "field1"));

        MySymbol var1 = new MySymbol(new Type("int", false), "var1", "1");
        MySymbol var2 = new MySymbol(new Type("int", false), "var2", "2");

        MySymbol param1 = new MySymbol(new Type("int", false), "param1", true, 1);
        MySymbol param2 = new MySymbol(new Type("int", false), "param2", true, 2);

        List<MySymbol> paramList = new ArrayList<MySymbol>();
        paramList.add(param1);
        paramList.add(param2);

        MethodScope func = new MethodScope(new Type("int", false), "main", paramList);


        st.addMethod("main", func);
        st.addMethod("function_1", func);

        System.out.println(st.getMethods());

        st.addImport("java.lang.System");
        st.addImport("java.lang.Math");

        System.out.println(st.getImports());


        st.getMethod("main").currentScope.addLocalVariable(var1);
        st.getMethod("main").newScope();
        st.getMethod("main").currentScope.addLocalVariable(var2);

        System.out.println(st.getMethod("main").getLocalVariables()); // Shows top scope
        System.out.println(st.getMethod("main").currentScope.getLocalVariables()); // Shows current scope


        System.out.println(st.getMethod("main").isLocalVariableInScope("var1")); // search in current scope and parent scopes


        st.getMethod("main").endScope();

        System.out.println(st.getMethod("main").currentScope.getLocalVariables()); // Shows top scope


        System.out.println(st.getMethod("main")); // Show method scope

        System.out.println(st); // Shows all methods and their scopes


    }
}
