package pt.up.fe.comp2023.SemanticVisitors;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;
import pt.up.fe.comp2023.SymbolTable.SymbolOrigin;

import java.util.List;

public class ExpressionVisitor extends AJmmVisitor<String, Type> {

    private final MySymbolTable st;
    private final List<Report> reports;

    public ExpressionVisitor(MySymbolTable table, List<Report> reports) {
        this.st = table;
        this.reports = reports;
    }


    @Override
    protected void buildVisitor() {
        addVisit("Paren", this::dealWithParentheses);
        addVisit("ArrayLookup", this::dealWithArrayLookup);
        addVisit("AttributeAccess", this::dealWithAttributeAccess);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("Not", this::dealWithNot);
        addVisit("NewIntArray", this::dealWithNewIntArray);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("BinaryComp", this::dealWithBinaryOp);
        addVisit("BinaryBool", this::dealWithBinaryOp);
        addVisit("This", this::dealWithThis);
        addVisit("Var", this::dealWithVar);
        addVisit("Boolean", this::dealWithBoolean);
        addVisit("Int", this::dealWithInt);
        setDefaultVisit(this::defaultVisit);
    }

    private Type defaultVisit(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, "");
        }
        return new Type("null", false);
    }

    private Type dealWithParentheses(JmmNode jmmNode, String s) {
        return visit(jmmNode.getJmmChild(0), "");
    }


    private Type dealWithAttributeAccess(JmmNode jmmNode, String s) {
        JmmNode left = jmmNode.getJmmChild(0);
        String right = jmmNode.get("atribute");
        Type leftType = visit(left, "");

        //Check if right is not length
        if (!(right.equals("length"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Array does not have attribute " + right));
            return new Type("error", false);
        }

        //Check if type of left is not array
        if (!leftType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), left.get("value") + " is not an array"));
            return new Type("error", false);
        }
        return new Type("int", false);
    }

    private Type dealWithArrayLookup(JmmNode jmmNode, String s) {
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);
        Type leftType = visit(left, "");
        Type rightType = visit(right, "");

        //Check if type of left is not array
        if (!leftType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Indexing error, " + left.get("var") + " is not an array"));
        }

        //Check if type of index is not int
        if (!rightType.equals(new Type("int", false))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Indexing error,index is not int"));
        }

        return new Type("int", false);
    }


    private Type dealWithMethodCall(JmmNode jmmNode, String s) {
        String methodName = jmmNode.get("method");
        JmmNode classCall = jmmNode.getJmmChild(0);
        Type classType = visit(classCall, "");


        if (classType.getName().equals(st.getClassName())) {
            //verify if method exist
            if (st.getMethods().contains(methodName)) {
                //verify arguments type
                List<Symbol> methodParams = st.getParameters(methodName);
                //Check if number of parameters is different from number of arguments
                if (methodParams.size() != (jmmNode.getNumChildren() - 1)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error in number of arguments calling method"));
                } else {
                    for (int i = 1; i < jmmNode.getNumChildren(); i++) {
                        Type argType = visit(jmmNode.getJmmChild(i), "");
                        // (i-1) because parameters index start at 0 and children that corresponds to arguments start at 1
                        if (!methodParams.get(i - 1).getType().equals(argType)) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error in argument type"));
                        }
                    }
                }
            }//checks if current class extends a super class
            else {
                if (st.getSuper().isEmpty()) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Method doesnt exist"));
                    return new Type("Error", false);
                } else {
                    return new Type("importCorrect", false);
                }
            }
        } else {
            //checks if class is imported assume method is being called correctly
            if (!(st.getImports().contains(classCall.get("var")) // static call
                    || st.getImports().contains(classType.getName()))) // virtual call
            {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Class not imported "));
                return new Type("importIncorrect", false);
            } else {
                return new Type("importCorrect", false);
            }
        }

        if (st.getReturnType(methodName) == null) {
            if (st.getImports().contains(classType.getName())) {
                return new Type("importCorrect", false);
            } else {
                return new Type("importIncorrect", false);
            }
        }

        Type type = st.getReturnType(methodName);
        return type;

    }

    private Type dealWithNot(JmmNode jmmNode, String s) {
        Type childType = visit(jmmNode.getJmmChild(0), "");

        //Check if child is boolean
        if (!childType.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Not operation expects boolean"));
        }

        return new Type("boolean", false);
    }

    private Type dealWithNewIntArray(JmmNode jmmNode, String s) {
        JmmNode child = jmmNode.getJmmChild(0);
        Type childType = visit(child, "");

        if (!childType.getName().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Array initialization requires integer size"));
        }
        return new Type("int", true);
    }

    private Type dealWithNewObject(JmmNode jmmNode, String s) {
        return new Type(jmmNode.get("objClass"), false);
    }

    private Type dealWithBinaryOp(JmmNode jmmNode, String s) {
        Type left = visit(jmmNode.getChildren().get(0));
        Type right = visit(jmmNode.getChildren().get(1));

        String op = jmmNode.get("op");

        if (!left.getName().equals(right.getName())) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Binary operation between different types in " + op + "operation"));
        } else if (left.isArray() || right.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Arrays cannot be used in binary operations"));
        } else if (!left.getName().equals("int") && (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("<"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Operation between" + op + " expects integer"));
        } else if (!left.getName().equals("boolean") && (op.equals("&&"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Operation between" + op + " expects boolean"));
        } else {
            return switch (op) {
                case "+", "-", "*", "/" -> new Type("int", false);
                case "&&", "<" -> new Type("boolean", false);
                default -> new Type("void", false);
            };
        }
        return new Type("void", false);
    }

    private Type dealWithThis(JmmNode jmmNode, String s) {
        if (st.getCurrentMethod().equals("main")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "This cannot be used in main method"));
        }
        return new Type(this.st.getClassName(), false);
    }

    private Type dealWithVar(JmmNode jmmNode, String s) {
        try {
            if (!st.findVar(jmmNode.get("var"), st.getCurrentMethod())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Var not present in ST"));
                return new Type("ERROR", false);
            }

            if (st.getCurrentMethod().equals("main")) {
                SymbolOrigin origin = st.getSymbolOrigin(jmmNode.get(("var")));
                if (origin == SymbolOrigin.FIELD) { // e se super
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Field in main"));
                }
            }

            var varAux = st.findTypeVar(jmmNode.get("var"), jmmNode);
            if (varAux == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "VarType couldn't be found"));
                return new Type("ERROR", false);
            }

            return varAux;
        } catch (Exception e) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), e.toString()));
            return new Type("null", false);
        }
    }

    private Type dealWithBoolean(JmmNode jmmNode, String s) {
        return new Type("boolean", false);
    }

    private Type dealWithInt(JmmNode jmmNode, String s) {
        return new Type("int", false);
    }


}
