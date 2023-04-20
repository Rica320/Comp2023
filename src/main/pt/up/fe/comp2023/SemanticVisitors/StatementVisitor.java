package pt.up.fe.comp2023.SemanticVisitors;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

import java.util.List;


public class StatementVisitor extends AJmmVisitor<String, Type> {
    private final MySymbolTable st;
    private final List<Report> reports;

    public StatementVisitor(MySymbolTable table, List<Report> reports) {
        this.st = table;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Scope", this::dealWithScope);
        addVisit("IfClause", this::dealWithIfConditional);
        addVisit("While", this::dealWithWhileConditional);
        addVisit("ExpressionStmt", this::dealWithExpressionStmt);
        addVisit("Assign", this::dealWithAssign);
        addVisit("ArrayAssign", this::dealWithArrayAssign);
        addVisit("ThenBlock", this::dealWithBlock);
        addVisit("ElseBlock", this::dealWithBlock);
        addVisit("WhileBlock", this::dealWithBlock);
        addVisit("MethodDecl", this::dealWithMethod);
        setDefaultVisit(this::defaultVisit);
    }


    private Type defaultVisit(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, "");
        }
        return new Type("null", false);
    }

    private Type dealWithMethod(JmmNode jmmNode, String s) {
        st.setCurrentMethod(jmmNode.get("name"));
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        return null;
    }

    private Type dealWithScope(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, "");
        }
        return new Type("null", false);
    }

    private Type dealWithIfConditional(JmmNode jmmNode, String s) {
        JmmNode child = jmmNode.getJmmChild(0);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        Type childType = expressionVisitor.visit(child, "");

        //check if type is not boolean
        if (!childType.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Conditional expression must be boolean"));
        }

        //visit statement inside if
        visit(jmmNode.getJmmChild(1));
        //visit statement inside else
        visit(jmmNode.getJmmChild(2));
        return childType;
    }

    private Type dealWithWhileConditional(JmmNode jmmNode, String s) {
        JmmNode child = jmmNode.getJmmChild(0);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        Type childType = expressionVisitor.visit(child, "");

        //check if type is not boolean
        if (!childType.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Conditional expression must return boolean"));
        }

        visit(jmmNode.getJmmChild(1));
        return childType;
    }

    private Type dealWithExpressionStmt(JmmNode jmmNode, String s) {
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        return expressionVisitor.visit(jmmNode.getJmmChild(0), "");
    }

    private Type dealWithAssign(JmmNode jmmNode, String s) {
        int line = Integer.parseInt(jmmNode.getJmmChild(0).get("lineStart"));
        int col = Integer.parseInt(jmmNode.getJmmChild(0).get("colStart"));

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        Type right = expressionVisitor.visit(jmmNode.getJmmChild(0), "");

        String left = jmmNode.get("var");

        if (!st.findVar(left, st.getCurrentMethod())) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Fields cannot be used in main method"));
            return new Type("ERROR", false);
        }

        JmmNode parent = jmmNode.getJmmParent();
        Type leftType = new Type("", false);

        while (!parent.getKind().equals("MethodDecl") && !parent.getKind().equals("MainMethod")) {
            parent = parent.getJmmParent();
        }

        String methodName;
        if (parent.getKind().equals("MethodDecl")) {
            methodName = parent.get("name");
        } else {
            methodName = "main";
        }

        //see if var is a local variable
        List<Symbol> localVariables = st.getLocalVariables(methodName);

        if (localVariables != null) {
            for (Symbol localVariable : localVariables) {
                if (localVariable.getName().equals(left)) {
                    leftType = localVariable.getType();
                    break;
                }
            }
        }

        //see if var is a parameter
        List<Symbol> parameters = st.getParameters(methodName);

        if (parameters != null && leftType.getName().equals("")) {
            for (Symbol parameter : parameters) {
                if (parameter.getName().equals(left)) {
                    leftType = parameter.getType();
                    break;
                }
            }
        }

        //see if var is a field
        List<Symbol> fields = st.getFields();
        if (fields != null && leftType.getName().equals("")) {
            for (Symbol field : fields) {
                if (field.getName().equals(left)) {
                    if (methodName.equals("main")) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Fields cannot be used in main method"));
                    }
                    leftType = field.getType();
                    break;
                }
            }
        }

        if (leftType.getName().equals(st.getSuper()) && right.getName().equals(st.getClassName())) {
            return right;
        }
        //if both are of type imported
        else if (st.getImports().contains(leftType.getName()) && st.getImports().contains(right.getName())) {
            return right;
        } else if (right.getName().equals("importCorrect")) {
            return right;
        } else if (!right.getName().equals(leftType.getName())) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Type of the assignee is not compatible to the assigned "));
            return new Type("ERROR", false);

        }
        return right;
    }

    private Type dealWithArrayAssign(JmmNode jmmNode, String s) {
        boolean isArr;
        try {
            isArr = st.findTypeVar(jmmNode.get("var")).isArray();
        } catch (Exception e) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Undefined array"));
            return new Type("error", false);
        }

        if (!isArr) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempting to assign a value to a non-array variable"));
            return new Type("error", false);
        }

        JmmNode left = jmmNode.getJmmChild(0);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        Type leftType = expressionVisitor.visit(left);

        if (!leftType.equals(new Type("int", false))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Index must be an int"));
            return new Type("error", false);
        }

        JmmNode right = jmmNode.getJmmChild(1);
        Type rightType = expressionVisitor.visit(right);


        if (!rightType.equals(new Type("int", false))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempting to assign a value that is not a int"));
            return new Type("error", false);
        }
        return null;
    }

    private Type dealWithBlock(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, "");
        }
        return new Type("null", false);
    }

}
