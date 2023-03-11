package pt.up.fe.comp2023.Visitor;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SymbolTable.MethodScope;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.ArrayList;
import java.util.List;

public class SymbolTableVisitor extends AJmmVisitor<String, String> {

    private final MySymbolTable st;

    public SymbolTableVisitor(MySymbolTable st) {
        this.st = st;
    }

    @Override
    public String visit(JmmNode jmmNode, String data) {
        return super.visit(jmmNode, data);
    }


    public MySymbolTable getSymbolTable() {
        return this.st;
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


        addVisit("MethodArgs", this::dealWithMethodArgs);
        addVisit("ParamDecl", this::dealWithParamDecl);
        addVisit("ReturnStmt", this::dealWithReturnStmt);

        // Type
        addVisit("IntArrayType", this::dealWithIntArrayType);
        addVisit("BooleanType", this::dealWithBooleanType);
        addVisit("IntType", this::dealWithIntType);
        addVisit("IdType", this::dealWithIdType);

        // Statement
        addVisit("Scope", this::dealWithScope);
        addVisit("If", this::dealWithIf);
        addVisit("While", this::dealWithWhile);
        addVisit("ExpressionStmt", this::dealWithExpressionStmt);
        addVisit("Assign", this::dealWithAssign);
        addVisit("ArrayAssign", this::dealWithArrayAssign);

        // Expression
        addVisit("Paren", this::dealWithParen);
        addVisit("Boolean", this::dealWithBoolean);
        addVisit("NewIntArray", this::dealWithNewIntArray);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("Not", this::dealWithNot);
        addVisit("ArrayLookup", this::dealWithArrayLookup);
        addVisit("ArrayLength", this::dealWithArrayLength);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("BinaryComp", this::dealWithBinaryComp);
        addVisit("BinaryBool", this::dealWithBinaryBool);
        addVisit("This", this::dealWithThis);
        addVisit("Var", this::dealWithVar);
        addVisit("ArrayLookup", this::dealWithArrayLookup);
        addVisit("Int", this::dealWithInt);


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

        // System.out.println("Adding field: " + new_symbol.getName() + " in method " + st.currentMethod);

        if (st.getCurrentMethodScope() == null) {
            st.addField(new_symbol);
        } else {
            st.getCurrentMethodScope().addLocalVariable(new_symbol);
        }

        return "";
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        MethodScope main = new MethodScope(new Type("void", false), "main", null); // TODO : STRING[] ??
        st.addMethod("main", main);

        st.setCurrentMethod("main");
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, s);
        }
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

    private String dealWithReturnStmt(JmmNode jmmNode, String s) {
        this.visit(jmmNode.getChildren().get(0), s); // Expression
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


    // ============================================ Statement ============================================

    private String dealWithScope(JmmNode jmmNode, String s) {
        // TODO: Ask prof if we need to create a new scope
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, s);
        }
        return "";
    }

    private String dealWithIf(JmmNode jmmNode, String s) {
        this.visit(jmmNode.getChildren().get(0), s); // Expression
        this.visit(jmmNode.getChildren().get(1), s); // Scope
        if (jmmNode.getChildren().size() == 3) {
            this.visit(jmmNode.getChildren().get(2), s); // Else
        }
        return "";
    }

    private String dealWithWhile(JmmNode jmmNode, String s) {
        this.visit(jmmNode.getChildren().get(0), s); // Expression
        this.visit(jmmNode.getChildren().get(1), s); // Scope
        return "";
    }

    private String dealWithExpressionStmt(JmmNode jmmNode, String s) {
        this.visit(jmmNode.getChildren().get(0), s); // Expression
        return "";
    }

    private String dealWithAssign(JmmNode jmmNode, String s) {
        String var = jmmNode.get("var");
        // String type = this.visit(jmmNode.getChildren().get(0), s);
        String value = jmmNode.getJmmChild(0).get("val");

        // TODO: we still need to check if the variable is a field or a local variable and if scope matters
        st.getCurrentMethodScope();//.setLocalVariableValue(var, value); ... TODO: comentei isto
        return "";
    }

    private String dealWithArrayAssign(JmmNode jmmNode, String s) {
        String var = jmmNode.get("var");
        String type = this.visit(jmmNode.getChildren().get(0), s);
        // TODO: same problem as above
        return "";
    }


    // ============================================ Expression ============================================

    private String dealWithParen(JmmNode jmmNode, String s) {
        return this.visit(jmmNode.getChildren().get(0), s);
    }

    private String dealWithBoolean(JmmNode jmmNode, String s) {
        return this.visit(jmmNode.getChildren().get(0), s);
    }

    private String dealWithNewIntArray(JmmNode jmmNode, String s) {
        return "int[]"; // TODO
    }

    private String dealWithNewObject(JmmNode jmmNode, String s) {
        return jmmNode.get("classID"); // TODO
    }

    private String dealWithNot(JmmNode jmmNode, String s) {
        return "boolean"; // TODO
    }

    private String dealWithArrayLookup(JmmNode jmmNode, String s) {
        return "int"; // TODO
    }

    private String dealWithArrayLength(JmmNode jmmNode, String s) {
        return "int"; // TODO
    }

    private String dealWithMethodCall(JmmNode jmmNode, String s) {
        // TODO
        return "";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        // TODO
        return "int";
    }

    private String dealWithBinaryComp(JmmNode jmmNode, String s) {
        // TODO
        return "boolean";
    }

    private String dealWithBinaryBool(JmmNode jmmNode, String s) {
        // TODO
        return "boolean";
    }

    private String dealWithThis(JmmNode jmmNode, String s) {
        // TODO how does this work?
        return "";
    }

    private String dealWithVar(JmmNode jmmNode, String s) {
        // TODO
        return "";
    }

    private String dealWithInt(JmmNode jmmNode, String s) {
        // TODO
        return "int";
    }


}
