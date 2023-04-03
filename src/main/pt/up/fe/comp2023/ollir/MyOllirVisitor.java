package pt.up.fe.comp2023.ollir;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.List;
import java.util.function.BiFunction;

public class MyOllirVisitor extends AJmmVisitor<String, Pair<String, String>> { // pair code/place

    private final MySymbolTable symbolTable;

    private int temp = 0;

    public MyOllirVisitor(SymbolTable symbolTable) {
        this.symbolTable = (MySymbolTable) symbolTable;
    }


    @Override
    protected void buildVisitor() {
        addVisit("ProgramRoot", this::dealWithProgram);

        // Class
        addVisit("ClassDecl", this::dealWithClassDecl);
        addVisit("MainMethod", this::dealWithMain);
        addVisit("MethodDecl", this::dealWithMethod);

        // Methods
        //addVisit("MethodArgs", this::dealWithMethodArgs);
        addVisit("ParamDecl", this::dealWithParamDecl);
        addVisit("MethodCall", this::dealWithMethodCall);

        addVisit("Int", this::dealWithInt);
        addVisit("Var", this::dealWithVar);

        // Statements
        addVisit("Assign", this::dealWithAssign);


        // Type
        addVisit("IntArrayType", this::dealWithIntArrayType);
        addVisit("BooleanType", this::dealWithBooleanType);
        addVisit("IntType", this::dealWithIntType);
        addVisit("IdType", this::dealWithIdType);

        // Expression
        addVisit("NewIntArray", this::dealWithNewIntArray);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("ReturnStmt", this::returnStmt);
        addVisit("BinaryOp", this::dealWithBinaryOp);

        setDefaultVisit(this::defaultVisit);
    }

    private int newTemp() {
        return temp++;
    }

    private Pair<String, String> dealWithBinaryOp(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        String left = this.visit(jmmNode.getJmmChild(0)).b;
        String right = this.visit(jmmNode.getJmmChild(1)).b;
        String op = jmmNode.get("op") + ".i32";
        // TODO: ver este
        String place = "t" + newTemp() + ".i32"; // TODO: BINARY OP IS .i32 ALL THE  TIME ???
        String code = sb.append(place).append(" :=.i32 ").append(left)
                .append(" ").append(op).append(" ")
                .append(right).append(";").toString();
        return new Pair<>(code, place); // TODO: TEMPORARY VALUES
    }

    private Pair<String, String> dealWithAssign(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        String varName = jmmNode.get("var");
        String value = this.visit(jmmNode.getJmmChild(0)).b; // TODO: adicionar codigo do lado esquerdo
        Type type = symbolTable.getCurrentMethodScope().getLocalVariable(varName).getType(); // TODO: e se n for local ...
        String ollirType = getOllirType(type.getName(), type.isArray());
        sb.append(varName).append(".").append(ollirType)
                .append(" :=.").append(ollirType).append(" ")
                .append(value); // TODO assumindo o valor da variavel da direita??
        return new Pair<>(sb.append(";\n").toString(), null);
    }

    private Pair<String, String> dealWithVar(JmmNode jmmNode, String s) {
        String varName = jmmNode.get("var");

        Type type = symbolTable.getCurrentMethodScope().getReturnType(); // TODO: assumindo que a semantica esta bem
        return new Pair<>("", varName + "." + getOllirType(type.getName(), type.isArray()));
    }

    private Pair<String, String> dealWithInt(JmmNode jmmNode, String s) { // TODO: VER SE O LADO ESQUERDO ESTA BEM
        return new Pair<>("", jmmNode.get("val") + ".i32");
    }

    private Pair<String, String> returnStmt(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        Type type = symbolTable.getReturnType(symbolTable.getCurrentMethod());
        String ret = getOllirType(type.getName(), type.isArray());
        Pair<String, String> expr = this.visit(jmmNode.getJmmChild(0));
        sb.append(expr.a).append("\n");
        sb.append("ret.").append(ret).append(" ").append(expr.b); //TODO: return value
        return new Pair<>(sb.append(";\n").toString(), null);
    }

    public static String getOllirType(String type, boolean isArray) {
        StringBuilder sb = new StringBuilder();
        if (isArray)
            sb.append("array.");
        return switch (type) {
            case "int" -> sb.append("i32").toString();
            case "boolean" -> sb.append("bool").toString();
            case "void", ".Any" -> sb.append("V").toString();
            default -> sb.append(type).toString();
        };
    }

    private Pair<String, String> dealWithMethodCall(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        String varName = jmmNode.getChildren().get(0).get("var");
        String methodName = jmmNode.get("method");

        if (symbolTable.isVariable(varName) || varName == null) {
            if (varName == null) {
                sb.append("invokevirtual(").append("this,\"").append(methodName).append("\"");
            } else {
                sb.append("invokevirtual(").append(varName).append(", \"").append(methodName).append("\"");
            }
        }else {
            sb.append("invokestatic(").append(varName).append(", \"").append(methodName).append("\"");
        }

        // List<Symbol> params = symbolTable.getCurrentMethodScope().getParameters(); // TODO: get params from method call
        // if (!params.isEmpty()) {
        //     for (Symbol param : params) {
        //         sb.append(param.getType().getName()).append(" ");
        //     }
        // }
        Type type = symbolTable.getReturnType(symbolTable.getCurrentMethod());

        sb.append(").")
                .append(getOllirType(type.getName(), type.isArray()))
                .append(";");

        return new Pair<>(sb.toString(), null);
    }

    private Pair<String, String> defaultVisit(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {
            Pair<String, String> childCode = this.visit(child, " ");
            if (childCode != null)
                sb.append(childCode.a);
        }
        return new Pair<>(sb.toString(), null);
    }

    private Pair<String, String> dealWithNewObject(JmmNode jmmNode, String s) {
        return null;
    }

    private Pair<String, String> dealWithNewIntArray(JmmNode jmmNode, String s) {
        return null;
    }

    private Pair<String, String> dealWithIdType(JmmNode jmmNode, String s) {
        return null;
    }

    private Pair<String, String> dealWithIntType(JmmNode jmmNode, String s) {
        return null;
    }

    private Pair<String, String> dealWithBooleanType(JmmNode jmmNode, String s) {
        return null;
    }

    private Pair<String, String> dealWithIntArrayType(JmmNode jmmNode, String s) {
        return null;
    }

    private Pair<String, String> dealWithParamDecl(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithMethodArgs(List<Symbol> symbols) {
        StringBuilder sb = new StringBuilder();
        if (!symbols.isEmpty()) {
            Symbol firstSym = symbols.get(0);
            sb.append(firstSym.getName()).append(".")
                    .append(getOllirType(firstSym.getType().getName(), firstSym.getType().isArray()));
            for (int i = 1; i < symbols.size(); i++) {
                Symbol sym = symbols.get(i);
                sb.append(", ")
                        .append(sym.getName()).append(".")
                        .append(getOllirType(sym.getType().getName(), sym.getType().isArray()));
            }
        }
        return sb.toString();
    }

    private Pair<String, String> dealWithMethod(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        String methodName = jmmNode.get("name");

        List<JmmNode> children = jmmNode.getChildren();

        symbolTable.setCurrentMethod(methodName);
        // TODO: OUTRA VEZ PUBLIC OU PRIVATE ?
        sb.append(".method public ").append(methodName).append("(")
                .append(dealWithMethodArgs(symbolTable.getCurrentMethodScope().getParameters()))
                .append(").").append(getOllirType(symbolTable.getReturnType(methodName).getName(),
                        symbolTable.getReturnType(methodName).isArray())).append(" {\n");

        sb.append(dealWithLocalVarDcl(symbolTable.getCurrentMethodScope().getLocalVariables()));

        for (int i = 0; i < children.size() - 1; i++) {
            Pair<String, String> childCode = this.visit(jmmNode.getJmmChild(i), " ");
            if (childCode != null)
                sb.append(childCode.a).append("\n");
        }
        sb.append(this.visit(children.get(children.size() - 1), " ").a);// TODO: change this, add the code before the return
        sb.append("}");
        symbolTable.setCurrentMethod(null);

        return new Pair<>(sb.toString(), null);
    }

    private Pair<String, String> dealWithMain(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        symbolTable.setCurrentMethod("main");
        List<Symbol> symbols = symbolTable.getCurrentMethodScope().getParameters();

        sb.append(".method public static main(").append(symbols.get(0).getName()).append(".array.String).V {\n");

        for (JmmNode child : jmmNode.getChildren()) {
            Pair<String, String> childCode = this.visit(child, " ");
            if (childCode != null)
                sb.append(childCode.a).append("\n");
        }
        sb.append("ret.V;");
        sb.append("\n}\n");

        symbolTable.setCurrentMethod(null);

        return new Pair<>( sb.toString(), null);
    }

    private String dealWithVarDcl(List<Symbol> symbols) {
        StringBuilder sb = new StringBuilder();
        for (Symbol symbol : symbols) { // TODO: PRIVATE or public????
            sb.append(".field public ")
                    .append(symbol.getName()).append(".")
                    .append(getOllirType(symbol.getType().getName(), symbol.getType().isArray())).append(";\n");
        }
        return sb.toString();
    }

    private String dealWithLocalVarDcl(List<Symbol> symbols) {
        StringBuilder sb = new StringBuilder();
        for (Symbol symbol : symbols) {
            Type type = symbol.getType();
            sb.append(symbol.getName()).append(".")
                    .append(getOllirType(type.getName(), type.isArray()))
                    .append(" :=.").append(getOllirType(type.getName(), type.isArray()))
                    .append(" 0.").append(getOllirType(type.getName(), type.isArray())) // TODO: 0 é default value?
                    .append(";\n");
        }
        return sb.toString();
    }

    private String defaultConstructor() {
        StringBuilder sb = new StringBuilder();
        sb.append(".construct ").append(symbolTable.getClassName()).append("().V {\n");
        sb.append("invokespecial(this, \"<init>\").V;\n").append("}\n");

        return sb.toString();
    }

    private Pair<String, String> dealWithClassDecl(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        String extendsClass = symbolTable.getSuper().isEmpty() ? "" : " extends " + symbolTable.getSuper();
        sb.append(symbolTable.getClassName()).append(extendsClass).append(" {\n\n");

        sb.append(dealWithVarDcl(symbolTable.getFields())).append("\n"); // TODO: ESTES \n sao para efeitos visuais
        sb.append(defaultConstructor());
        for (JmmNode child : jmmNode.getChildren()) {
            String childCode = this.visit(child, " ").a;
            if (childCode != null)
                sb.append(childCode);
        }

        sb.append("\n}");
        return new Pair<>(sb.toString(), null);
    }

    private String dealWithImports() {
        StringBuilder sb = new StringBuilder();
        for (String impr : symbolTable.getImports())
            sb.append("import ").append(impr).append(";\n");

        return sb.toString();
    }

    private Pair<String, String> dealWithProgram(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(dealWithImports()).append("\n");
        List<JmmNode> children = jmmNode.getChildren();

        Pair<String, String> childCode = this.visit(children.get(children.size() -1), " ");
        if (childCode != null)
            sb.append(childCode.a);

        return new Pair<>(sb.toString(), null);
    }

    @Override
    public Pair<String, String> visit(JmmNode jmmNode) {
        return super.visit(jmmNode);
    }

    @Override
    public void addVisit(Object kind, BiFunction<JmmNode, String, Pair<String, String>> method) {
        super.addVisit(kind, method);
    }
}
