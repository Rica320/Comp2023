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

import java.awt.*;
import java.util.List;

//Type verification
//Array access is done over an array
public class StatementVisitor extends AJmmVisitor<String, Type> {
    private final MySymbolTable st;
    private final List<Report> reports;

    public StatementVisitor(MySymbolTable table, List<Report> reports){
        this.st = table;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        addVisit("MainMethod", this::dealWithMain);
        addVisit("MethodDecl", this::dealWithMethod);

        addVisit("IfClause", this::dealWithIfConditional);
        addVisit("While", this::dealWithWhileConditional);
        addVisit("ExpressionStmt", this::dealWithExpressionStmt);
        addVisit("Assign", this::dealWithAssign);
        addVisit("ArrayAssign", this::dealWithArrayAssign);
        setDefaultVisit(this::defaultVisit);
    }

    private Type dealWithMethod(JmmNode jmmNode, String s) {
        st.setCurrentMethod(jmmNode.get("name"));
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        return null;
    }

    private Type dealWithMain(JmmNode jmmNode, String s) {
        st.setCurrentMethod("main");
        defaultVisit(jmmNode, s);
        st.setCurrentMethod(null);
        return null;
    }

    private Type defaultVisit(JmmNode jmmNode, String s){
        for(JmmNode child : jmmNode.getChildren()){
            visit(child, "");
        }
        return new Type("null", false);
    }

    private Type dealWithIfConditional(JmmNode jmmNode, String s){
        JmmNode child = jmmNode.getJmmChild(0);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st,reports);
        Type childType = expressionVisitor.visit(child, "");

        //Checks if type is not boolean
        if(!childType.getName().equals("boolean")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Conditional expression must be boolean" ));
        }

        //Visit statement inside if
        visit(jmmNode.getJmmChild(1));
        //visit statement inside else
        visit(jmmNode.getJmmChild(2));
        return childType;
    }

    private Type dealWithWhileConditional(JmmNode jmmNode, String s){
        JmmNode child = jmmNode.getJmmChild(0);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st,reports);
        Type childType = expressionVisitor.visit(child, "");

        //Checks if type is not boolean
        if(!childType.getName().equals("boolean")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Conditional expression must return boolean" ));
        }

        //Visit statement inside while
        visit(jmmNode.getJmmChild(1));
        return childType;
    }

    private Type dealWithExpressionStmt(JmmNode jmmNode, String s){
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        expressionVisitor.visit(jmmNode.getJmmChild(0), "");
        return new Type("null", false);
    }

    private Type dealWithAssign(JmmNode jmmNode, String s){
        /*Type leftT = st.findTypeVar(jmmNode.get("var"));

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        Type rightType = expressionVisitor.visit(jmmNode.getJmmChild(0), "");

        boolean lIsImp = st.hasImport(leftT.getName());
        boolean rIsImp = st.hasImport(rightType.getName());

        boolean extendsClass = (rightType.getName().equals(st.getClassName()) && leftT.getName().equals(st.getSuper()));

        if (!(leftT.equals(rightType) || extendsClass) && !(lIsImp && rIsImp)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempting to assign two different types: " + leftT.getName() + "," + rightType.getName()));
            return new Type("error", false);
        }
        return new Type("null", false);
        */

        int line = Integer.parseInt(jmmNode.getJmmChild(0).get("lineStart"));
        int col = Integer.parseInt(jmmNode.getJmmChild(0).get("colStart"));

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st, reports);
        Type right = expressionVisitor.visit(jmmNode.getJmmChild(0), "");

        String left = jmmNode.get("var"); //ver tipo do var

        JmmNode parent = jmmNode.getJmmParent();
        Type leftType= new Type("", false);

        while(!parent.getKind().equals("MethodDecl") && !parent.getKind().equals("MainMethod")) {
            parent = parent.getJmmParent();
        }

        String methodName;
        if(parent.getKind().equals("MethodDecl")){
            methodName = parent.get("name");
        }
        else{
            methodName = "main";
        }
        //see if var is a field
        List<Symbol> fields = st.getFields();
        if(fields != null){
            for (int i = 0; i < fields.size(); i++){
                if(fields.get(i).getName().equals(left)){
                    if(methodName.equals("main")){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Fields cannot be used in main method"));
                    }
                    leftType = fields.get(i).getType();
                    break;
                }
            }
        }

        //see if var is a parameter
        List<Symbol> parameters = st.getParameters(methodName);

        if(parameters != null){
            for (int i = 0; i < parameters.size(); i++){
                if(parameters.get(i).getName().equals(left)){
                    leftType = parameters.get(i).getType();
                    break;
                }
            }
        }

        //see if var is a local variable
        List<Symbol> localVariables = st.getLocalVariables(methodName);

        if(localVariables != null){
            for (int i = 0; i < localVariables.size(); i++){
                if(localVariables.get(i).getName().equals(left)){
                    leftType = localVariables.get(i).getType();
                    break;
                }
            }
        }

        //check if left (assignee) is superclass and right (assigned) is the current class
        if(leftType.getName().equals(st.getSuper()) && right.getName().equals(st.getClassName())){
            return right;
        }
        //if both are of type that are imported
        else if(st.getImports().contains(leftType.getName()) && st.getImports().contains(right.getName())){
            return right;
        }
        else if(right.getName().equals("importCorrect")){
            return right;
        }
        else if(!right.getName().equals(leftType.getName())){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Type of the assignee must be compatible with the assigned "));
            return new Type("ERROR", false);

        }
        return right;

    }

    private Type dealWithArrayAssign(JmmNode jmmNode, String s) {

        boolean isArr = st.findTypeVar(jmmNode.get("var")).isArray();

        if (!isArr) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempting to assign a value to a non-array variable" ));
            return new Type("error", false);
        }

        JmmNode left = jmmNode.getJmmChild(0);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(st,reports);
        Type leftType = expressionVisitor.visit(left);

        if (leftType.getName().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Index must be an int" ));
            return new Type("error", false);
        }

        JmmNode right = jmmNode.getJmmChild(1);
        Type rightType = expressionVisitor.visit(right, "");

        if (!rightType.getName().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempting to assign a value that is not a int"));
            return new Type("error", false);
        }
        return new Type("null", false);
    }
}
