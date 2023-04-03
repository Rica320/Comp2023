package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.List;
import java.util.function.BiFunction;

public class MyOllirVisitor extends AJmmVisitor<String, String> {

    private final MySymbolTable symbolTable;

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


        // Type
        addVisit("IntArrayType", this::dealWithIntArrayType);
        addVisit("BooleanType", this::dealWithBooleanType);
        addVisit("IntType", this::dealWithIntType);
        addVisit("IdType", this::dealWithIdType);

        // Expression
        addVisit("NewIntArray", this::dealWithNewIntArray);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("ReturnStmt", this::returnStmt);

        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithVar(JmmNode jmmNode, String s) {
        String varName = jmmNode.get("var");

        Type type = symbolTable.getCurrentMethodScope().getReturnType(); // TODO: assumindo que a semantica esta bem
        return varName + "." + getOllirType(type.getName(), type.isArray());
    }

    private String dealWithInt(JmmNode jmmNode, String s) {
        return jmmNode.get("val") + ".i32";
    }

    private String returnStmt(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        Type type = symbolTable.getReturnType(symbolTable.getCurrentMethod());
        String ret = getOllirType(type.getName(), type.isArray());
        sb.append("ret.").append(ret).append(" ").append(this.visit(jmmNode.getJmmChild(0))); //TODO: return value
        return sb.append(";\n").toString();
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

    private String dealWithMethodCall(JmmNode jmmNode, String s) {
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

        return sb.toString();
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

    private String dealWithMethod(JmmNode jmmNode, String s) {
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
        sb.append(this.visit(children.get(children.size() - 1), " "));
        sb.append("}\n");
        symbolTable.setCurrentMethod(null);

        return sb.toString();
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        symbolTable.setCurrentMethod("main");
        List<Symbol> symbols = symbolTable.getCurrentMethodScope().getParameters();

        sb.append(".method public static main(").append(symbols.get(0).getName()).append(".array.String).V {\n");

        for (JmmNode child : jmmNode.getChildren()) {
            String childCode = this.visit(child, " ");
            if (childCode != null)
                sb.append(childCode).append("\n");
        }
        sb.append("ret.V;");
        sb.append("\n}");

        symbolTable.setCurrentMethod(null);

        return sb.toString();
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

    private String dealWithClassDecl(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        String extendsClass = symbolTable.getSuper() == null ? "" : " extends " + symbolTable.getSuper();
        sb.append(symbolTable.getClassName()).append(extendsClass).append(" {\n\n");

        sb.append(dealWithVarDcl(symbolTable.getFields())).append("\n"); // TODO: ESTES \n sao para efeitos visuais
        sb.append(defaultConstructor());
        for (JmmNode child : jmmNode.getChildren()) {
            String childCode = this.visit(child, " ");
            if (childCode != null)
                sb.append(childCode);
        }

        sb.append("\n}");
        return sb.toString();
    }

    private String dealWithImports() {
        StringBuilder sb = new StringBuilder();
        for (String impr : symbolTable.getImports())
            sb.append("import ").append(impr).append(";\n");

        return sb.toString();
    }

    private String dealWithProgram(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(dealWithImports()).append("\n");
        List<JmmNode> children = jmmNode.getChildren();

        String childCode = this.visit(children.get(children.size() -1), " ");
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
