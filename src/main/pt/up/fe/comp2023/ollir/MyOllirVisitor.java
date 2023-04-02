package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.function.BiFunction;

public class MyOllirVisitor extends AJmmVisitor<String, String> {

    private final SymbolTable symbolTable;

    public MyOllirVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }


    @Override
    protected void buildVisitor() {
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
        addVisit("MethodCall", this::dealWithMethodCall);


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

    private String dealWithMethodCall(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();


        return null;
    }

    private String defaultVisit(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {
            String childCode = this.visit(child, " ");
            if (childCode != null)
                sb.append(childCode);
        }
        return sb.toString();
    }

    private String dealWithNewObject(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithNewIntArray(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithIdType(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithIntType(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithBooleanType(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithIntArrayType(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithParamDecl(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithMethodArgs(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithMethod(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        List<Symbol> symbols = symbolTable.getParameters("main");

        sb.append(".method public static main(").append(symbols.get(0).getName()).append(".array.String).V {");

        for (JmmNode child : jmmNode.getChildren()) {
            String childCode = this.visit(child, " ");
            if (childCode != null)
                sb.append(childCode);
        }
        sb.append("\n}");
        return sb.toString();
    }

    private String dealWithVarDcl(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithClassDecl(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        sb.append("class ").append(symbolTable.getClassName()).append(" {\n");

        for (JmmNode child : jmmNode.getChildren()) {
            String childCode = this.visit(child, " ");
            if (childCode != null)
                sb.append(childCode);
        }

        sb.append("\n}");
        return sb.toString();
    }

    private String dealWithImports(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        for (String impr : symbolTable.getImports())
            sb.append("import ").append(impr).append(";\n");

        return sb.toString();
    }

    private String dealWithProgram(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        String imports = this.visit(jmmNode.getJmmChild(0), " ");
        if (imports != null)
            sb.append(imports);

        String childCode = this.visit(jmmNode.getJmmChild(1), " ");
        if (childCode != null)
            sb.append(childCode);

        return sb.toString();
    }

    @Override
    public String visit(JmmNode jmmNode) {
        return super.visit(jmmNode);
    }

    @Override
    public void addVisit(Object kind, BiFunction<JmmNode, String, String> method) {
        super.addVisit(kind, method);
    }
}
