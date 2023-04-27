package pt.up.fe.comp2023.OptimizeVisitors;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SymbolTable.MethodScope;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.ArrayList;
import java.util.List;

public class ConstantPropagation extends AJmmVisitor<String, String> {

    private final MySymbolTable st;

    public ConstantPropagation(MySymbolTable st) {
        this.st = st;
    }

    @Override
    public String visit(JmmNode jmmNode, String data) {
        return super.visit(jmmNode, data);
    }


    @Override
    protected void buildVisitor() {
        // Add visit methods
        addVisit("ProgramRoot", this::dealWithProgram);
        addVisit("ImportDecl", this::dealWithImports);

        // Class
        addVisit("ClassDecl", this::dealWithClassDecl);
        addVisit("VarDcl", this::dealWithVarDcl);
        addVisit("MainMethod", this::dealWithMain);
        addVisit("MethodDecl", this::dealWithMethod);

        // Methods
        addVisit("MethodArgs", this::dealWithMethodArgs);
        addVisit("ParamDecl", this::dealWithParamDecl);


        // Type
        addVisit("IntArrayType", this::dealWithIntArrayType);
        addVisit("BooleanType", this::dealWithBooleanType);
        addVisit("IntType", this::dealWithIntType);
        addVisit("IdType", this::dealWithIdType);

        // Expression
        addVisit("NewIntArray", this::dealWithNewIntArray);
        addVisit("NewObject", this::dealWithNewObject);

        setDefaultVisit(this::defaultVisit);
    }

    private String defaultVisit(JmmNode jmmNode, String s) {
        return "DEFAULT_VISIT";
    }


    private String dealWithProgram(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, s); // Imports + Class
        }
        return "";
    }

    // ============================================ Imports + Class ============================================

    private String dealWithImports(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {
            if (sb.length() > 0) {
                sb.append(".");
            }

            sb.append(child.get("packageID"));
        }
        st.addImport(sb.toString());
        return "";
    }

    private String dealWithClassDecl(JmmNode jmmNode, String s) {
        st.setClassName(jmmNode.get("name"));

        try {
            st.setSuperClass(jmmNode.get("superName"));
        } catch (Exception e) {
            st.setSuperClass("");
        }

        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, s);
        }
        return "";
    }

    private String dealWithVarDcl(JmmNode jmmNode, String s) {
        JmmNode type_node = jmmNode.getChildren().get(0);
        String type = this.visit(type_node, s);
        boolean isArr = type.charAt(type.length() - 1) == ']';

        if (isArr) type = type.substring(0, type.length() - 2);

        Symbol new_symbol = new Symbol(new Type(type, isArr), jmmNode.get("var"));


        if (st.getCurrentMethodScope() == null) {
            st.addField(new_symbol);
        } else {
            st.addLocalVariable(st.getCurrentMethod(), new_symbol);
        }

        return "";
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        MethodScope main = new MethodScope(new Type("void", false), "main", null);
        st.addMethod("main", main);

        st.setCurrentMethod("main");
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, s);
        }

        Symbol new_symbol = new Symbol(new Type("String", true), jmmNode.get("arg"));
        st.getCurrentMethodScope().setParameters(List.of(new_symbol));
        st.setCurrentMethod(null);

        return "";
    }

    private String dealWithMethod(JmmNode jmmNode, String s) {
        JmmNode type_node = jmmNode.getChildren().get(0);
        String type = this.visit(type_node, s);
        boolean isArr = type.charAt(type.length() - 1) == ']';

        MethodScope method;
        if (isArr) type = type.substring(0, type.length() - 2);

        method = new MethodScope(new Type(type, isArr), jmmNode.get("name"), null);

        st.addMethod(jmmNode.get("name"), method);

        st.setCurrentMethod(jmmNode.get("name"));
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, s);
        }
        st.setCurrentMethod(null);

        return "";
    }

    private String dealWithMethodArgs(JmmNode jmmNode, String s) {
        List<Symbol> list = new ArrayList<>();
        st.getCurrentMethodScope().setParameters(list);
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, s);
        }
        return "";
    }

    private String dealWithParamDecl(JmmNode jmmNode, String s) {

        JmmNode type_node = jmmNode.getChildren().get(0);
        String type = this.visit(type_node, s);
        boolean isArr = type.charAt(type.length() - 1) == ']';

        Symbol param;
        if (isArr) type = type.substring(0, type.length() - 2);

        param = new Symbol(new Type(type, isArr), jmmNode.get("var"));

        st.getCurrentMethodScope().addParameter(param);
        return "";
    }

    // ============================================ Type ============================================

    private String dealWithIntArrayType(JmmNode jmmNode, String s) {
        return "int[]";
    }

    private String dealWithBooleanType(JmmNode jmmNode, String s) {
        return "boolean";
    }

    private String dealWithIntType(JmmNode jmmNode, String s) {
        return "int";
    }

    private String dealWithIdType(JmmNode jmmNode, String s) {
        return jmmNode.get("name");
    }


    // ============================================ Expression ============================================

    private String dealWithNewIntArray(JmmNode jmmNode, String s) {
        Symbol symbol = new Symbol(new Type("int", true), jmmNode.getJmmParent().get("var"));
        st.addLocalVariable(st.getCurrentMethod(), symbol);
        return "int[]";
    }

    private String dealWithNewObject(JmmNode jmmNode, String s) {
        Symbol symbol = new Symbol(new Type(jmmNode.get("objClass"), false), jmmNode.getJmmParent().get("var"));
        st.addLocalVariable(st.getCurrentMethod(), symbol);
        return jmmNode.get("objClass");
    }


}
