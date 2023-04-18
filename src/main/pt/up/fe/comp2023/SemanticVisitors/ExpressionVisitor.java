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

        addVisit("MainMethod", this::dealWithMain);
        addVisit("MethodDecl", this::dealWithMethod);

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

    private Type defaultVisit(JmmNode jmmNode, String s) {
        for(JmmNode child : jmmNode.getChildren()){
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

        if(!(right.equals("length"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Array does not have attribute " + right));
            return new Type("error", false);
        }

        if(!leftType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), left.get("value") + " is not an array"));
            return new Type("error", false);
        }
        return new Type("int",false);
    }

    private Type dealWithArrayLookup(JmmNode jmmNode, String s) {
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);
        Type leftType = visit(left, "");
        Type rightType = visit(right, "");

        //Check if type of left is not array
        if(!leftType.isArray()){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Indexing error, " + left.get("var") + " is not an array"));
        }

        //Check if type of index is not int
        if(!rightType.getName().equals("int")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Indexing error,index is not int"));
        }

        return new Type("int",false);
    }

   // private boolean checkArgsTyps(JmmNode jmmNode, String method) {
   //     List<Symbol> methodParams = st.getParameters(method);
   //     for (int i = 1; i < jmmNode.getNumChildren(); i++) {
   //         Type argType = visit(jmmNode.getJmmChild(i), "");
   //         // (i-1) because parameters index start at 0 and children that corresponds to arguments start at 1
   //         if (!methodParams.get(i - 1).getType().equals(argType)) {
   //             reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error in argument type"));
   //             return false;
   //         }
   //     }
//
   //     return true;
   // }

    private Type dealWithMethodCall(JmmNode jmmNode, String s) {

        /*String method = jmmNode.get("method");
        JmmNode classCall = jmmNode.getJmmChild(0);
        Type classType = visit(classCall, "");

        if(classType == null){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Method " + method + " null"));
            return new Type("error", false);
        }

        //check if class is imported
        if (st.getImports().contains(classType.getName())) {
            return classType;
        }

        // check if is extended
        if (st.getSuper().equals(classType.getName())) {
            return classType;
        }

        // super relation to extended class
        if (classType.getName().equals(st.getClassName()) && st.hasSuperClass()) {
            return classType;
        }

        // verify if class exists
        if ( classType.getName().equals(st.getClassName())) {
            // verify if method exists
            if (!st.hasMethod(method) ) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Method " + method + " does not exist"));
                return new Type("error", false);
            }

            // verify number of args
            if (jmmNode.getChildren().size() != st.getMethod(method).getParameters().size() + 1) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")),
                        Integer.parseInt(jmmNode.get("colStart")), "Method " + method + " expects " + st.getMethod(method).getParameters().size() + " arguments"));
                return new Type("error", false);
            }

            // verify types of args
            if (jmmNode.getChildren().size() > 1) {
                List<Symbol> methodParams = st.getParameters(method);

                for (int i = 1; i < jmmNode.getChildren().size(); i++) {
                    Type argType = visit(jmmNode.getJmmChild(i), "");

                    if(!methodParams.get(i-1).getType().equals(argType)) {
                    //if (!argType.getName().equals(st.getMethod(method).getParameters().get(i-1).getType().toString())) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Method " + method + " expects " + st.getMethod(method).getParameters().get(i - 1).getType() + " as argument " + i  + " but got " + argType.getName()));
                        return new Type("error", false);
                    }
                    //System.out.println("Method " + method + " expects " + st.getMethod(method).getParameters().get(i - 1).getType() + " as argument " + i  + " but got " + argType.getName());

                }
            }

        }


        try{
            return st.getReturnType(method);
        }catch (Exception e){
            return new Type("null", false);
        }*/



        String methodName = jmmNode.get("method");
        JmmNode classCall = jmmNode.getJmmChild(0);
        Type classType = visit(classCall,"");
        System.out.println("method name: " + methodName);

        //The class calling the method is the current class
        if(classType.getName().equals(st.getClassName())){
            //verify if method exist
            if(st.getMethods().contains(methodName)){
                //verify arguments type
                List<Symbol> methodParams = st.getParameters(methodName);
                //Check if number of parameters is different from number of arguments
                if(methodParams.size() != (jmmNode.getNumChildren() - 1)){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error in number of arguments calling method" ));
                }else{
                    for (int i = 1; i < jmmNode.getNumChildren(); i++){
                        Type argType = visit(jmmNode.getJmmChild(i),"");
                        // (i-1) because parameters index start at 0 and children that corresponds to arguments start at 1
                        if(!methodParams.get(i - 1).getType().equals(argType)){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error in argument type" ));
                        }
                    }
                }
            }//checks if current class extends a super class
            else{
                System.out.println("super3: " + st.getSuper());
                System.out.println("empty: "+st.getSuper().isEmpty());

                if(st.getSuper().isEmpty()){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Method doesnt exist"));
                    return new Type("Error", false);
                }else{
                    return new Type("importCorrect", false);
                }
            }
        }else{
            //checks if class is imported assume method is being called correctly
            if(!st.getImports().contains(classType.getName())){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Class not imported"));
                return new Type("importIncorrect", false);
            }else{
                return new Type("importCorrect", false);
            }
        }

        if(st.getReturnType(methodName) == null){
            if(st.getImports().contains(classType.getName())){
                return new Type("importCorrect", false);
            }else{
                return new Type("importIncorrect", false);
            }
        };

        return st.getReturnType(methodName);

    }

    private Type dealWithNot(JmmNode jmmNode, String s) {
        Type childType = visit(jmmNode.getJmmChild(0), "");

        //Check if child is boolean
        if(!childType.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Not operation expects boolean"));
        }

        return new Type("boolean", false);
    }

    private Type dealWithNewIntArray(JmmNode jmmNode, String s) {
        JmmNode child = jmmNode.getJmmChild(0);
        Type childType = visit(child, "");

        if(!childType.getName().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Array initialization requires integer size"));
        }
        return new Type("int",true);
    }

    private Type dealWithNewObject(JmmNode jmmNode, String s) {
        return new Type(jmmNode.get("objClass"), false);
    }

    private Type dealWithBinaryOp(JmmNode jmmNode, String s) {
        Type left = visit(jmmNode.getChildren().get(0));
        Type right = visit(jmmNode.getChildren().get(1));

        String op = jmmNode.get("op");
        System.out.println(left.getName() + " " + op + " " + right.getName());
        if (!left.getName().equals(right.getName())) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Binary operation between different types in " + op + "operation" ));
        } else if (left.isArray() || right.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Arrays cannot be used in binary operations"));
        } else if (!left.getName().equals("int") && (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("<"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Operation between" + op + " expects integer"));
        } else if (!left.getName().equals("boolean") && (op.equals("&&"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Operation between" + op + " expects boolean"));
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
        if(st.getCurrentMethod().equals("main")) {
            //testar isto
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "This cannot be used in main method"));
        }
        return new Type(this.st.getClassName(),false);
    }

    private Type dealWithVar(JmmNode jmmNode, String s) {
        /*
        try {
            if (st.getCurrentMethod().equals("main")) {
                SymbolOrigin origin = st.getSymbolOrigin(jmmNode.get(("var")));
                if (origin == SymbolOrigin.FIELD ) { // e se super
                    reports.add(
                            new Report(ReportType.ERROR, Stage.SEMANTIC,
                                    Integer.parseInt(jmmNode.get("lineStart")),
                                    Integer.parseInt(jmmNode.get("colStart")), "Field in main"));
                }
            }

            return st.findTypeVar(jmmNode.get("var"));
        } catch (Exception e) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")), e.toString()));
            return new Type("null", false);
        }

        */
        //Get identifier name
        String name = jmmNode.get("var");
        //Get parent
        JmmNode parent = jmmNode.getJmmParent();
        while(!parent.getKind().equals("MethodDecl") && !parent.getKind().equals("MainMethod")){
            parent = parent.getJmmParent();
        }

        String methodName = "";
        if(parent.getKind().equals("MainMethod")){
            methodName = "main";
        }
        else if(parent.getKind().equals("MethodDecl")){
            methodName = parent.get("name");
        }

        //Get local variables from method
        List<Symbol> locals = st.getLocalVariables(methodName);
        //Check if identifier is a local variable
        if(locals != null){
            for (Symbol l : locals){
                if(l.getName().equals(name)){
                    return l.getType();
                }
            }
        }

        //Get params from method
        List<Symbol> parameters  = st.getParameters(methodName);
        //Check if identifier is a parameter
        if(parameters != null){
            for (Symbol p: parameters){
                if(p.getName().equals(name)){
                    return p.getType();
                }
            }
        }

        //Get fields from class
        List<Symbol> fields = st.getFields();
        //Check if identifier is a field
        if(fields != null){
            for(Symbol f:fields){
                if(f.getName().equals(name)){
                    //Do I need to verify if the method is main? Fields cannot be used in main method?
                    if(methodName.equals("main")){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Fields cannot be used inside static method main"));
                    }
                    return  f.getType();
                }
            }
        }

        //No imports or Variable not in the imports.
        if(st.getImports() == null || !st.getImports().contains(name)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Variable " + name + " not declared"));
        }else{
            return new Type(name, false);
        }

        //Dummy return
        return new Type("errorIdentifier", false);



    }

    private Type dealWithBoolean(JmmNode jmmNode, String s) {
        return new Type("boolean",false);
    }
    private Type dealWithInt(JmmNode jmmNode, String s) {
        return new Type("int",false);
    }


}
