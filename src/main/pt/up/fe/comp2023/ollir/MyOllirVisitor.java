package pt.up.fe.comp2023.ollir;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SymbolTable.MethodScope;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;
import pt.up.fe.comp2023.SymbolTable.SymbolOrigin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class MyOllirVisitor extends AJmmVisitor<String, Pair<String, String>> { // pair code/place

    private final MySymbolTable symbolTable;

    private int temp = 0;
    private int label = 0;

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
        addVisit("Boolean", this::dealWithBool);

        // Statements
        addVisit("Assign", this::dealWithAssign);
        addVisit("IfClause", this::dealWithIf);


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
        addVisit("BinaryComp", this::dealWithBinaryComp);
        addVisit("BinaryBool", this::dealWithBinaryComp);
        addVisit("Paren", this::dealWithParen);
        addVisit("AtributeAccess", this::dealWithAtributeAccess);

        setDefaultVisit(this::defaultVisit);
    }

    private Pair<String, String> dealWithIf(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        Pair<String, String> condition = this.visit(jmmNode.getJmmChild(0));
        Pair<String, String> then = this.visit(jmmNode.getJmmChild(1));
        Pair<String, String> els = this.visit(jmmNode.getJmmChild(2));

        // TODO: outra vez a questão de n estar a utilizar as variaveis de uma forma eficiente
        sb.append(condition.a).append("\n");
        String label = newLabel();
        String thenLabel = "Then" + label;
        String endLabel = "End" + label;
        sb.append("if (").append(condition.b).append(") goto ").append(thenLabel).append(";\n");
        sb.append(els.a).append("\ngoto ").append(endLabel).append(";\n")
                .append(thenLabel).append(":\n").append(then.a).append("\n")
                .append(endLabel).append(":\n");

        return new Pair<>(sb.toString(), "");
    }

    private String newLabel() {
        return String.valueOf(label++);
    }

    private Pair<String, String> dealWithAtributeAccess(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        Pair<String, String> left = this.visit(jmmNode.getJmmChild(0));

        sb.append(left.a); // code that "creates" the child

        String place = "t" + newTemp() + ".i32";
        sb.append(place).append(" :=.i32 arraylength(").append(left.b)
                .append(").i32;\n");

        return new Pair<>(sb.toString(), place);
    }

    private Pair<String, String> dealWithParen(JmmNode jmmNode, String s) {
        return this.visit(jmmNode.getJmmChild(0));
    }


    private Pair<String, String> dealWithBool(JmmNode jmmNode, String s) {
        String val = Objects.equals(jmmNode.get("val"), "true") ? "1" : "0";
        return new Pair<>("", val + ".bool");
    }

    private Pair<String, String> dealWithBinaryComp(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        Pair<String, String> left = this.visit(jmmNode.getJmmChild(0));
        Pair<String, String> right = this.visit(jmmNode.getJmmChild(1));
        String op = jmmNode.get("op") + ".bool";

        String place = "t" + newTemp() + ".bool";

        sb.append(left.a).append("\n");
        sb.append(right.a).append("\n");

        String code = sb.append(place).append(" :=.bool ").append(left.b)
                .append(" ").append(op).append(" ")
                .append(right.b).append(";").toString();
        return new Pair<>(code, place);
    }

    private int newTemp() {
        return temp++;
    }

    private Pair<String, String> dealWithBinaryOp(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        Pair<String, String> left = this.visit(jmmNode.getJmmChild(0));
        Pair<String, String> right = this.visit(jmmNode.getJmmChild(1));
        String op = jmmNode.get("op") + ".i32";
        // TODO: ver este
        String place = "t" + newTemp() + ".i32"; // TODO: BINARY OP IS .i32 ALL THE  TIME ???

        sb.append(left.a).append("\n"); // TODO para poupar temps, verificar se o left.a é vazio
        sb.append(right.a).append("\n");

        String code = sb.append(place).append(" :=.i32 ").append(left.b)
                .append(" ").append(op).append(" ")
                .append(right.b).append(";").toString();
        return new Pair<>(code, place);
    }

    private Pair<String, String> dealWithAssign(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        String varName = jmmNode.get("var");

        Pair<String, String> codePlace = this.visit(jmmNode.getJmmChild(0));
        Type type = symbolTable.getCurrentMethodScope().getLocalVariable(varName).getType(); // TODO: e se n for local ...
        String ollirType = getOllirType(type.getName(), type.isArray());

        // TODO: we can improve the number of temps by modifying the code bellow ... a := 2 + 1 instead of t1 := 2 + 1; a := t1
        sb.append(codePlace.a).append("\n"); // APPENDED CODE THAT GENERATES THE TEMP ON THE RIGHT OF THE ASSIGN
        sb.append(varName).append(".").append(ollirType)
                .append(" :=.").append(ollirType).append(" ")
                .append(codePlace.b); // TODO assumindo o valor da variavel da direita??

        return new Pair<>(sb.append(";\n").toString(), null);
    }

    private Pair<String, String> dealWithVar(JmmNode jmmNode, String s) {
        String varName = jmmNode.get("var");

        SymbolOrigin symbolOrign = symbolTable.getSymbolOrigin(varName);
        Type type = findTypeVar(varName); // TODO: assumindo que a semantica esta bem

        switch (symbolOrign) { // TODO:::: e se houver uma var chamada t1 ?????
            case PARAMETER: // already checks STATIC
                return new Pair<>("", "$" + symbolTable.getParameterIndex(varName) + "." + varName + ".i32");
            case IMPORT:
            case LOCAL:
                return new Pair<>("", varName + "." + getOllirType(type.getName(), type.isArray()));
            case FIELD:
                StringBuilder sb = new StringBuilder();
                String ollirType = getOllirType(type.getName(), type.isArray());
                String newTemp = "t" + newTemp() + "." + ollirType;
                sb.append(newTemp).append(" :=.")
                        .append(ollirType).append(" getfield(this,")
                        .append(varName).append(".").append(ollirType).append(").").append(ollirType).append(";\n");
                return new Pair<>(sb.toString(), newTemp);
        }
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

    public Type findTypeVar(String varName) {
        Symbol symbol = symbolTable.getCurrentMethodScope().getLocalVariable(varName); // Is it a local variable?
        if (symbol == null) {
            symbol = symbolTable.getField(varName); // Is it a field?
        }
        if (symbol == null) {
            symbol = symbolTable.getCurrentMethodScope().getParameter(varName); // Is it a parameter ?
        }
        if (symbolTable.hasImport(varName)) { // Is it an import? ... TODO: always void ?
            return new Type("void", false);
        }
        return symbol.getType();
    }

    public Type findRetMethod(String methodName) {
        MethodScope symbol = symbolTable.getMethod(methodName);
        if (symbol == null) {
            return new Type("void", false);
        }
        return symbol.getReturnType(); // TODO: é suposto assumir que é void ???
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
    // TODO ::: VER ISTO ... t4.V :=.V invokestatic(io, "println", c.i32).V;
    private Pair<String, String> dealWithMethodCall(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        String varName = jmmNode.getJmmChild(0).getKind().equals("This") ? null : jmmNode.getJmmChild(0).get("var");
        String methodName = jmmNode.get("method");

        Type type = findRetMethod(methodName);
        String ollirType = getOllirType(type.getName(), type.isArray());

        String newTemp = null;
        List<JmmNode> params = jmmNode.getChildren();
        List<Pair<String, String>> codePlace = new ArrayList<>();
        for (int i = 1; i < params.size(); i++) {
            codePlace.add(this.visit(params.get(i)));
            sb.append(codePlace.get(i - 1).a).append("\n");
        }

        if (!ollirType.equals("V")) { // TODO: DISCUTIR ISTO COM O STOR ... para dif de void tem de ter uma temp (ou var)
            newTemp = "t" + newTemp() + "." + ollirType;
            sb.append(newTemp).append(" :=.").append(ollirType).append(" ");
        }
        if (symbolTable.isVariable(varName) || varName == null) {
            if (varName == null) {
                sb.append("invokevirtual(").append("this,\"").append(methodName).append("\"");
            } else {
                Type typeVar = findTypeVar(varName);
                sb.append("invokevirtual(").append(varName)
                        .append(".").append(getOllirType(typeVar.getName(), typeVar.isArray()))
                        .append(", \"").append(methodName).append("\"");
            }
        }else {
            sb.append("invokestatic(").append(varName).append(", \"").append(methodName).append("\"");
        }

        if (params.size() > 1) {
           for (Pair<String,String> codeP : codePlace) {
               sb.append(", ").append(codeP.b);
           }
        }

        sb.append(").")
                .append(ollirType)
                .append(";");

        return new Pair<>(sb.toString(), newTemp);
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

    private Pair<String, String> dealWithNewObject(JmmNode jmmNode, String s) { // TODO: otimizações
        // TODO: especial é para method new object
        StringBuilder sb = new StringBuilder();
        String className = jmmNode.get("objClass");
        String newTemp = "t" + newTemp() + "." + className;
        sb.append(newTemp).append(" :=.").append(className).append(" ");
        sb.append("new(").append(className).append(").").append(className).append(";\n")
                .append("invokespecial(").append(newTemp)
                .append(", \"<init>\").V;");
        return new Pair<>(sb.toString(), newTemp);
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

        sb.append("\n.method public static main(").append(symbols.get(0).getName()).append(".array.String).V {\n");

        /*
         TODO: o que acontece no caso Simple e; ... fica s.Simple :=.Simple 0.Simple; ? ou seja nulo?
         */
        sb.append(dealWithLocalVarDcl(symbolTable.getCurrentMethodScope().getLocalVariables()));
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
