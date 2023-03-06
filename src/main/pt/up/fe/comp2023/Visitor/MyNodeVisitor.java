package pt.up.fe.comp2023.Visitor;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SymbolTable.MethodScope;
import pt.up.fe.comp2023.SymbolTable.MySymbol;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.ArrayList;
import java.util.List;

public class MyNodeVisitor extends AJmmVisitor<String, String> {

    private MySymbolTable st;

    public MyNodeVisitor(MySymbolTable st) {
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
      /*
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

        */


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
        for (JmmNode child : jmmNode.getChildren()) {
            st.addImport(child.get("packageID"));
        }
        return "";
    }

    private String dealWithClassDecl(JmmNode jmmNode, String s) {
        st.setClassName(jmmNode.get("name"));

        try {
            st.setSuperClass(jmmNode.get("extends"));
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

        MySymbol new_symbol = new MySymbol(new Type(type, isArr), jmmNode.get("var"));

        // System.out.println("Adding field: " + new_symbol.getName() + " in method " + st.currentMethod);

        if (st.currentMethod == null) {
            st.addField(new_symbol);
        } else {
            st.getMethod(st.currentMethod).addLocalVariable(new_symbol);
        }

        return "";
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        MethodScope main = new MethodScope(new Type("void", false), "main", null);
        st.addMethod("main", main);

        st.currentMethod = "main";
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, s);
        }
        st.currentMethod = null;

        return "";
    }

    private String dealWithMethod(JmmNode jmmNode, String s) {
        JmmNode type_node = jmmNode.getChildren().get(0);
        String type = this.visit(type_node, s);
        boolean isArr = type.charAt(type.length() - 1) == ']';

        MethodScope method = new MethodScope(new Type(type, isArr), jmmNode.get("name"), null);
        st.addMethod(jmmNode.get("name"), method);

        st.currentMethod = jmmNode.get("name");
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, s);
        }
        st.currentMethod = null;

        return "";
    }

    private String dealWithMethodArgs(JmmNode jmmNode, String s) {
        List<MySymbol> list = new ArrayList<MySymbol>();
        st.getMethod(st.currentMethod).setParameters(list);
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, s);
        }
        return "";
    }

    private String dealWithParamDecl(JmmNode jmmNode, String s) {

        JmmNode type_node = jmmNode.getChildren().get(0);
        String type = this.visit(type_node, s);
        boolean isArr = type.charAt(type.length() - 1) == ']';

        MySymbol param = new MySymbol(new Type(type, isArr), "param", null);

        st.getMethod(st.currentMethod).addParameter(param);
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

    /*
    // ============================================ Statement ============================================

    private String dealWithScope(JmmNode jmmNode, String s) {
        st.addScope();
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, s);
        }
        st.removeScope();
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
        String type = this.visit(jmmNode.getChildren().get(0), s);
        st.addVar(var, type);
        return "";
    }

    private String dealWithArrayAssign(JmmNode jmmNode, String s) {
        String var = jmmNode.get("var");
        String type = this.visit(jmmNode.getChildren().get(0), s);
        st.addVar(var, type);
        return "";
    }


    // ============================================ Expression ============================================

    private String dealWithParen(JmmNode jmmNode, String s) {
        return this.visit(jmmNode.getChildren().get(0), s);
    }

    private String dealWithBoolean(JmmNode jmmNode, String s) {
        return "boolean";
    }

    private String dealWithNewIntArray(JmmNode jmmNode, String s) {
        return "int[]";
    }

    private String dealWithNewObject(JmmNode jmmNode, String s) {
        return jmmNode.get("classID");
    }

    private String dealWithNot(JmmNode jmmNode, String s) {
        return "boolean";
    }

    private String dealWithArrayLookup(JmmNode jmmNode, String s) {
        return "int";
    }

    private String dealWithArrayLength(JmmNode jmmNode, String s) {
        return "int";
    }

    private String dealWithMethodCall(JmmNode jmmNode, String s) {
        return st.getMethodType(jmmNode.get("methodID"), jmmNode.get("classID"));
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        return "int";
    }

    private String dealWithBinaryComp(JmmNode jmmNode, String s) {
        return "boolean";
    }

    private String dealWithBinaryBool(JmmNode jmmNode, String s) {
        return "boolean";
    }

    private String dealWithThis(JmmNode jmmNode, String s) {
        return st.getClassID();
    }

    private String dealWithVar(JmmNode jmmNode, String s) {
        return st.getVarType(jmmNode.get("varID"));
    }

    private String dealWithInt(JmmNode jmmNode, String s) {
        return "int";
    }

    */


}