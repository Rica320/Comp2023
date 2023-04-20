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

    public static String getOllirType(String type, boolean isArray) {
        StringBuilder sb = new StringBuilder();
        if (isArray) sb.append("array.");
        return switch (type) {
            case "int" -> sb.append("i32").toString();
            case "boolean" -> sb.append("bool").toString();
            case "void" -> sb.append("V").toString();
            default -> sb.append(type).toString();
        };
    }

    public static boolean placeVariable(JmmNode node) {
        return !node.getJmmParent().getKind().equals("ExpressionStmt"); // TODO; ver melhor
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
        addVisit("While", this::dealWithWhile);
        addVisit("ArrayAssign", this::dealWithArrayAssign);


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
        addVisit("AttributeAccess", this::dealWithAtributeAccess);
        addVisit("Not", this::dealWithNot);
        addVisit("ArrayLookup", this::dealWithArrayLookup);
        addVisit("This", this::dealWithThis);

        setDefaultVisit(this::defaultVisit);
    }

    private Pair<String, String> dealWithThis(JmmNode jmmNode, String s) {
        String tempName = "t" + newTemp() + "." + symbolTable.getClassName();
        String sb = tempName + " :=." + symbolTable.getClassName() + " $0.this." + symbolTable.getClassName() + ";\n";

        return new Pair<>(sb, tempName);
    }

    private Pair<String, String> dealWithArrayLookup(JmmNode jmmNode, String s) {

        StringBuilder sb = new StringBuilder();
        StringBuilder code = new StringBuilder();
        Pair<String, String> array;
        Pair<String, String> index;
        String arrayName;

        if (jmmNode.hasAttribute("var")) {
            array = new Pair<>("", jmmNode.get("var"));
            arrayName = array.b;
            index = this.visit(jmmNode.getJmmChild(0));
        } else {
            array = this.visit(jmmNode.getJmmChild(0));
            int isParam = array.b.charAt(0) == '$' ? 1 : 0;
            arrayName = array.b.split("\\.")[isParam];
            index = this.visit(jmmNode.getJmmChild(1));
        }

        code.append(array.a).append("\n");
        code.append(index.a).append("\n");

        SymbolOrigin origin = symbolTable.getSymbolOrigin(arrayName);
        return arrLookup(arrayName, index, origin, code, sb);
    }

    private boolean isConstant(String s) {
        return s.split("\\.")[0].matches("\\d+");
    }

    private Pair<String, String> arrLookup(String arrayName, Pair<String, String> index, SymbolOrigin origin, StringBuilder code, StringBuilder sb) {
        if (isConstant(index.b)) {
            String tempName = "t" + newTemp() + ".i32";
            code.append(tempName).append(" :=.i32 ").append(index.b).append(";\n");
            index = new Pair<>("", tempName);
        }
        switch (origin) {
            case FIELD:
                String tempName = "t" + temp++;
                code.append(tempName).append(".array.i32 :=.array.i32 getfield(this, ").append(arrayName).append(".array.i32).array.i32;");
                return new Pair<>(code.toString(), sb.append(tempName).append("[").append(index.b).append("].i32").toString());
            case PARAMETER:
                sb.append("$").append(symbolTable.getParameterIndex(arrayName)).append(".").append(arrayName).append("[").append(index.b).append("].i32");
                return new Pair<>(code.toString(), sb.toString());
            case IMPORT:
                throw new RuntimeException("ArrayLookup: IMPORT");
            case LOCAL:
            default:
                return new Pair<>(code.toString(), sb.append(arrayName).append("[").append(index.b).append("].i32").toString());
        }
    }

    private Pair<String, String> dealWithArrayAssign(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        Pair<String, String> index = this.visit(jmmNode.getJmmChild(0));
        if (index.b.split("\\.")[0].matches("\\d+")) { // is a number
            String tempName = "t" + temp++ + ".i32";
            sb.append(tempName).append(" :=.i32 ").append(index.b).append(";\n");
            index = new Pair<>(index.a, tempName);
        }
        Pair<String, String> value = this.visit(jmmNode.getJmmChild(1));

        String varName = jmmNode.get("var");
        Type type = symbolTable.findTypeVar(varName, jmmNode);

        SymbolOrigin origin = symbolTable.getSymbolOrigin(varName);

        Pair<String, String> la = arrLookup(varName, index, origin, sb, new StringBuilder());

        String olliType = getOllirType(type.getName(), false);

        sb.append(index.a).append("\n");
        sb.append(value.a).append("\n");
        sb.append(la.b).append(" :=.").append(olliType).append(" ").append(value.b).append(";\n");

        return new Pair<>(sb.toString(), null);
    }


    private Pair<String, String> dealWithWhile(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        String newLabel = newLabel();
        sb.append("Loop").append(newLabel).append(":\n");
        Pair<String, String> condition = this.visit(jmmNode.getJmmChild(0));
        sb.append(condition.a).append("\n");
        sb.append("if (!.bool ").append(condition.b).append(") goto End").append(newLabel).append(";\n");
        Pair<String, String> body = this.visit(jmmNode.getJmmChild(1));
        sb.append(body.a).append("\ngoto Loop").append(newLabel).append(";\n");
        sb.append("End").append(newLabel).append(":\n");
        return new Pair<>(sb.toString(), null);
    }

    private Pair<String, String> dealWithNot(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        Pair<String, String> expr = this.visit(jmmNode.getJmmChild(0));
        sb.append(expr.a).append("\n");
        String temp = "t" + newTemp() + ".bool";
        sb.append(temp).append(" :=.bool ").append("!.bool ").append(expr.b).append(";\n");
        return new Pair<>(sb.toString(), temp);

    }

    private Pair<String, String> dealWithIf(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        Pair<String, String> condition = this.visit(jmmNode.getJmmChild(0));
        Pair<String, String> then = this.visit(jmmNode.getJmmChild(1));
        Pair<String, String> els = this.visit(jmmNode.getJmmChild(2));

        sb.append(condition.a).append("\n");
        String label = newLabel();
        String thenLabel = "Then" + label;
        String endLabel = "End" + label;
        sb.append("if (").append(condition.b).append(") goto ").append(thenLabel).append(";\n");
        sb.append(els.a).append("\ngoto ").append(endLabel).append(";\n").append(thenLabel).append(":\n").append(then.a).append("\n").append(endLabel).append(":\n");

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
        sb.append(place).append(" :=.i32 arraylength(").append(left.b).append(").i32;\n");

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

        String code = sb.append(place).append(" :=.bool ").append(left.b).append(" ").append(op).append(" ").append(right.b).append(";").toString();
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
        String place = "t" + newTemp() + ".i32";

        sb.append(left.a).append("\n");
        sb.append(right.a).append("\n");

        String code = sb.append(place).append(" :=.i32 ").append(left.b).append(" ").append(op).append(" ").append(right.b).append(";").toString();
        return new Pair<>(code, place);
    }

    private Pair<String, String> dealWithAssign(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        String varName = jmmNode.get("var");

        Pair<String, String> codePlace = this.visit(jmmNode.getJmmChild(0));
        SymbolOrigin symbolOrign = symbolTable.getSymbolOrigin(varName);
        Type type = symbolTable.findTypeVar(varName, jmmNode);
        String ollirType = getOllirType(type.getName(), type.isArray());

        sb.append(codePlace.a).append("\n");

        switch (symbolOrign) {
            case PARAMETER -> // already checks STATIC
                    sb.append("$").append(symbolTable.getParameterIndex(varName)).append(".").append(varName).append(".i32").append(" :=.").append(ollirType).append(" ").append(codePlace.b); // TODO: FALAR COM STOR
            case IMPORT, LOCAL ->
                    sb.append(varName).append(".").append(ollirType).append(" :=.").append(ollirType).append(" ").append(codePlace.b);
            case FIELD -> {
                sb.append("putfield(this,").append(varName).append(".").append(ollirType).append(", ").append(codePlace.b).append(").").append("V;\n"); // TODO: .V ????
                return new Pair<>(sb.toString(), null);
            }
            default -> throw new IllegalStateException("Unexpected value: " + symbolOrign);
        }

        return new Pair<>(sb.append(";\n").toString(), null);
    }

    private Pair<String, String> dealWithVar(JmmNode jmmNode, String s) {
        String varName = jmmNode.get("var");

        SymbolOrigin symbolOrign = symbolTable.getSymbolOrigin(varName);
        Type type = symbolTable.findTypeVar(varName, jmmNode);

        switch (symbolOrign) {
            case PARAMETER -> { // already checks STATIC
                return new Pair<>("", "$" + symbolTable.getParameterIndex(varName) + "." + varName + "." + getOllirType(type.getName(), type.isArray()));
            }// TODO: IMPORTS
            case IMPORT, LOCAL -> {
                return new Pair<>("", varName + "." + getOllirType(type.getName(), type.isArray()));
            }
            case FIELD, UNKNOWN -> {
                StringBuilder sb = new StringBuilder();
                String ollirType = getOllirType(type.getName(), type.isArray());
                String newTemp = "t" + newTemp() + "." + ollirType;
                sb.append(newTemp).append(" :=.").append(ollirType).append(" getfield(this,").append(varName).append(".").append(ollirType).append(").").append(ollirType).append(";\n");
                return new Pair<>(sb.toString(), newTemp);
            }
        }
        return new Pair<>("", varName + "." + getOllirType(type.getName(), type.isArray()));
    }

    private Pair<String, String> dealWithInt(JmmNode jmmNode, String s) {
        return new Pair<>("", jmmNode.get("val") + ".i32");
    }

    private Pair<String, String> returnStmt(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        Type type = symbolTable.getReturnType(symbolTable.getCurrentMethod());
        String ret = getOllirType(type.getName(), type.isArray());
        Pair<String, String> expr = this.visit(jmmNode.getJmmChild(0));
        sb.append(expr.a).append("\n");
        sb.append("ret.").append(ret).append(" ").append(expr.b);
        return new Pair<>(sb.append(";\n").toString(), null);
    }

    public Type findRetMethod(String methodName, JmmNode node) {
        MethodScope symbol = symbolTable.getMethod(methodName);
        if (symbol == null) {
            if (!node.hasAttribute("expType")) {
                return new Type("void", false);
            }
            return new Type(node.get("expType"), node.get("expType").contains("["));
        }
        return symbol.getReturnType();
    }

    private Pair<String, String> dealWithMethodCall(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        Pair<String, String> codePlace1 = this.visit(jmmNode.getJmmChild(0));

        String varName = jmmNode.getJmmChild(0).getKind().equals("This") ? null : codePlace1.b;
        String typeVar = jmmNode.getJmmChild(0).getKind().equals("This") ? "this" : codePlace1.b.split("\\.")[0];
        sb.append(codePlace1.a).append("\n");

        if (varName != null) { // TODO: refactor
            int isParam = codePlace1.b.charAt(0) == '$' ? 1 : 0;
            varName = "";
            if (isParam == 1) varName = codePlace1.b.split("\\.")[0] + ".";
            varName += codePlace1.b.split("\\.")[isParam];
            typeVar = codePlace1.b.split("\\.")[isParam + 1];
        }

        String methodName = jmmNode.get("method");

        Type type = findRetMethod(methodName, jmmNode);
        String ollirType = getOllirType(type.getName(), type.isArray());

        SymbolOrigin symbolOrign = symbolTable.getSymbolOrigin(varName);

        String newTemp = null;
        List<JmmNode> params = jmmNode.getChildren();
        List<Pair<String, String>> codePlace = new ArrayList<>();
        for (int i = 1; i < params.size(); i++) {
            Pair<String, String> code = this.visit(params.get(i));
            sb.append(code.a).append("\n");
            if (code.b.contains("[")) {  // REFACTOR
                String newTemp2 = "t" + newTemp() + ".i32";
                sb.append(newTemp2).append(" :=.i32 ").append(code.b).append(";\n");
                code = new Pair<>(null, newTemp2); // code does not matter in this case
            }
            codePlace.add(code);

        }

        if (!ollirType.equals("V") && placeVariable(jmmNode)) {
            newTemp = "t" + newTemp() + "." + ollirType;
            sb.append(newTemp).append(" :=.").append(ollirType).append(" ");
        }
        if (varName == null || symbolOrign != SymbolOrigin.IMPORT) {
            if (varName == null) {
                sb.append("invokevirtual(").append("this,\"").append(methodName).append("\"");
            } else {
                //Type typeVar = findTypeVar(varName);
                sb.append("invokevirtual(").append(varName).append(".").append(getOllirType(typeVar, false)).append(", \"").append(methodName).append("\"");
            }
        } else {
            sb.append("invokestatic(").append(varName).append(", \"").append(methodName).append("\"");
        }

        if (params.size() > 1) {
            for (Pair<String, String> codeP : codePlace) {
                sb.append(", ").append(codeP.b);
            }
        }

        sb.append(").").append(ollirType).append(";");

        return new Pair<>(sb.toString(), newTemp);
    }

    private Pair<String, String> defaultVisit(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {
            Pair<String, String> childCode = this.visit(child, " ");
            if (childCode != null) sb.append(childCode.a);
        }
        return new Pair<>(sb.toString(), null);
    }

    private Pair<String, String> dealWithNewObject(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        String className = jmmNode.get("objClass");
        String newTemp = "t" + newTemp() + "." + className;
        sb.append(newTemp).append(" :=.").append(className).append(" ");
        sb.append("new(").append(className).append(").").append(className).append(";\n").append("invokespecial(").append(newTemp).append(", \"<init>\").V;");
        return new Pair<>(sb.toString(), newTemp);
    }

    private Pair<String, String> dealWithNewIntArray(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();
        Pair<String, String> codeSizeTmp = this.visit(jmmNode.getJmmChild(0), " ");
        sb.append(codeSizeTmp.a).append("\n");
        String newTemp = "t" + newTemp() + ".array.i32";
        sb.append(newTemp).append(" :=.array.i32 new(array,").append(codeSizeTmp.b).append(").array.i32;\n");
        return new Pair<>(sb.toString(), newTemp);
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
            sb.append(firstSym.getName()).append(".").append(getOllirType(firstSym.getType().getName(), firstSym.getType().isArray()));
            for (int i = 1; i < symbols.size(); i++) {
                Symbol sym = symbols.get(i);
                sb.append(", ").append(sym.getName()).append(".").append(getOllirType(sym.getType().getName(), sym.getType().isArray()));
            }
        }
        return sb.toString();
    }

    private Pair<String, String> dealWithMethod(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        String methodName = jmmNode.get("name");

        List<JmmNode> children = jmmNode.getChildren();

        symbolTable.setCurrentMethod(methodName);
        sb.append(".method public ").append(methodName).append("(").append(dealWithMethodArgs(symbolTable.getCurrentMethodScope().getParameters()));
        sb.append(").").append(getOllirType(symbolTable.getReturnType(methodName).getName(), symbolTable.getReturnType(methodName).isArray())).append(" {\n");
        sb.append(dealWithLocalVarDcl(symbolTable.getCurrentMethodScope().getLocalVariables()));

        for (int i = 0; i < children.size() - 1; i++) {
            Pair<String, String> childCode = this.visit(jmmNode.getJmmChild(i), " ");
            if (childCode != null) sb.append(childCode.a).append("\n");
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

        sb.append(dealWithLocalVarDcl(symbolTable.getCurrentMethodScope().getLocalVariables()));

        for (JmmNode child : jmmNode.getChildren()) {
            Pair<String, String> childCode = this.visit(child, " ");
            if (childCode != null) sb.append(childCode.a).append("\n");
        }

        sb.append("ret.V;").append("\n}\n");

        symbolTable.setCurrentMethod(null);

        return new Pair<>(sb.toString(), null);
    }

    private String dealWithVarDcl(List<Symbol> symbols) {
        StringBuilder sb = new StringBuilder();
        for (Symbol symbol : symbols)
            sb.append(".field public ").append(symbol.getName()).append(".").append(getOllirType(symbol.getType().getName(), symbol.getType().isArray())).append(";\n");
        return sb.toString();
    }

    private String dealWithLocalVarDcl(List<Symbol> symbols) {
        StringBuilder sb = new StringBuilder();
        for (Symbol symbol : symbols) {
            Type type = symbol.getType();
            if (!type.isArray() && (type.getName().equals("int") || type.getName().equals("boolean")))
                sb.append(symbol.getName()).append(".").append(getOllirType(type.getName(), type.isArray())).append(" :=.").append(getOllirType(type.getName(), type.isArray())).append(" 0.").append(getOllirType(type.getName(), type.isArray())) // TODO: 0 é default value?
                        .append(";\n");
        }
        return sb.toString();
    }

    private String defaultConstructor() {
        return ".construct " + symbolTable.getClassName() + "().V {\n" + "invokespecial(this, \"<init>\").V;\n" + "}\n";
    }

    private Pair<String, String> dealWithClassDecl(JmmNode jmmNode, String s) {
        StringBuilder sb = new StringBuilder();

        String extendsClass = symbolTable.getSuper().isEmpty() ? "" : " extends " + symbolTable.getSuper();
        sb.append(symbolTable.getClassName()).append(extendsClass).append(" {\n");

        sb.append(dealWithVarDcl(symbolTable.getFields())).append("\n");
        sb.append(defaultConstructor());

        for (JmmNode child : jmmNode.getChildren()) {
            String childCode = this.visit(child, " ").a;
            if (childCode != null) sb.append(childCode);
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

        Pair<String, String> childCode = this.visit(children.get(children.size() - 1), " ");
        if (childCode != null) sb.append(childCode.a);

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
