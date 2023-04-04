package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.HashMap;

// • Assignments
//• Arithmetic operations (with correct precedence)
//• Method invocation

public class MyJasminBackend implements JasminBackend {

    ClassUnit classe;
    String code = "";

    HashMap<String, Descriptor> currVarTable;

    int labelCounter = 0;

    private String getRegister(String var) {
        if (currVarTable.containsKey(var)) return String.valueOf(currVarTable.get(var).getVirtualReg());
        return "-1";
    }

    private String getNewLabel() {
        return "label_" + labelCounter++;
    }

    private String toJasminType(String type) {
        switch (type) {
            case "VOID" -> {
                return "V";
            }
            case "INT32" -> {
                return "I";
            }
            case "BOOLEAN" -> {
                return "Z";
            }
            case "ARRAYREF" -> {
                return "[I";
            }
            case "OBJECTREF" -> {
                return "Ljava/lang/Object;";
            }
            case "STRING" -> {
                return "Ljava/lang/String;";
            }
            default -> {
                return "TypeError(" + type + ")";
            }
        }
    }

    private void showClass() {
        System.out.println("\n<CLASS>");
        System.out.println("\tNAME: " + this.classe.getClassName());
        System.out.println("\tEXTENDS: " + this.classe.getSuperClass());
        System.out.println("\tFIELDS: " + this.classe.getFields());
        System.out.println("<END CLASS/>\n");

        this.classe.getMethods().forEach(method -> {
            System.out.println("\n<METHOD>");
            System.out.println("\tMETHOD: " + method.getMethodName());
            System.out.println("\tPARAMS: " + method.getParams());
            System.out.println("\tRETURN TYPE: " + method.getReturnType());
            System.out.println("\t===================INSTRUCTIONS===================");
            method.getInstructions().forEach(instruction -> {
                System.out.println("\t\t");
                instruction.show();
            });
            System.out.println("\t===================<INSTRUCTIONS/>===================");
            System.out.println("<END METHOD/>");
        });
    }


    private void loadElement(StringBuilder codeBuilder, Element element) {
        boolean isNumber = element.getType().getTypeOfElement().equals(ElementType.INT32);
        boolean isBoolean = element.getType().getTypeOfElement().equals(ElementType.BOOLEAN);

        if (isNumber || isBoolean) {
            if (element.isLiteral()) {
                LiteralElement literal = (LiteralElement) element;
                int value = Integer.parseInt(literal.getLiteral());
                if (value < 6) codeBuilder.append("iconst_");  // more efficient than bipush
                else if (value < 128) codeBuilder.append("bipush ");
                else codeBuilder.append("ldc ");
                codeBuilder.append(value);
                codeBuilder.append("\n\t");
                return;
            } else { // variable
                if (isBoolean && (((Operand) element).getName().equals("true") || ((Operand) element).getName().equals("false"))) {
                    String boolVal = ((Operand) element).getName();
                    codeBuilder.append(boolVal.equals("true") ? "iconst_1" : "iconst_0");
                    codeBuilder.append(" ; ").append(boolVal).append("\n\t");
                    return;
                }
                codeBuilder.append("iload ").append(getRegister(((Operand) element).getName()));
                codeBuilder.append("\n\t");
                return;
            }
        } else if (element.isLiteral()) { // string
            codeBuilder.append("ldc ").append(((LiteralElement) element).getLiteral());
            codeBuilder.append("\n\t");
            return;
        }

        // variable or array
        Operand variable = (Operand) element;
        String name = variable.getName();
        codeBuilder.append("aload ").append(getRegister(name)).append(" ; ").append(name);
        codeBuilder.append("\n\t");

    }

    public String addPrint(String str) {
        return "\tgetstatic java/lang/System/out Ljava/io/PrintStream;\n\t" + "ldc \"" + str + "\"\n\t" + "invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V\n";
    }


    private String addHeaders() {
        code += ".class public " + this.classe.getClassName() + "\n";
        if (this.classe.getSuperClass() != null) code += ".super " + this.classe.getSuperClass() + "\n";
        else {
            code += ".super java/lang/Object\n";
        }
        code += "\n";
        return code;
    }

    private String addImports() {
        StringBuilder imports = new StringBuilder();

        if (this.classe.getImports().size() == 0) return "; No imports\n";

        for (String imp : this.classe.getImports()) {
            imports.append(".import ").append(imp).append("\n");
        }
        return imports.toString();
    }

    public String addFields() {
        StringBuilder codeBuilder = new StringBuilder();
        if (this.classe.getFields().size() == 0) return "; No fields\n";
        this.classe.getFields().forEach(field -> codeBuilder.append(".field private ").append(field.getFieldName()).append(" ").append(toJasminType(field.getFieldType().toString())).append("\n"));
        return codeBuilder.toString();
    }

    public String addConstructor() {
        return """
                \n.method public <init>()V
                \taload_0
                \tinvokenonvirtual java/lang/Object/<init>()V
                \treturn
                .end method
                """;
    }


    private String addMethods() {
        StringBuilder codeBuilder = new StringBuilder();
        this.classe.getMethods().forEach(method -> {

            currVarTable = method.getVarTable();

            if (method.getMethodName().equals("main"))
                codeBuilder.append("\n\n; main method\n.method public static main([Ljava/lang/String;)V\n");
            else if (method.getMethodName().equals(this.classe.getClassName())) return; // ignore constructor
            else codeBuilder.append("\n.method public ").append(method.getMethodName()).append("(");

            if (!method.getMethodName().equals("main"))
                method.getParams().forEach(param -> codeBuilder.append(toJasminType(param.getType().toString())));

            if (!method.getMethodName().equals("main")) { // ignore constructor because its already defined
                String returnType = method.getReturnType().toString();
                codeBuilder.append(")").append(toJasminType(returnType)).append("\n");
            }

            // in this phase we don't need to worry about locals and stack limits
            codeBuilder.append("\t.limit stack 30\n");
            codeBuilder.append("\t.limit locals 30\n\n");

            // add instructions
            method.getInstructions().forEach(instruction -> codeBuilder.append(addInstruction(instruction)).append("\n"));

            currVarTable = null;

            if (method.getMethodName().equals("main")) {
                codeBuilder.append(addPrint("Program finished"));
                codeBuilder.append("\treturn\n");
            }

            codeBuilder.append(".end method\n\n");
        });
        return codeBuilder.toString();
    }


    private String addInstructionAssign(AssignInstruction instruction) {
        StringBuilder codeBuilder = new StringBuilder();

        codeBuilder.append("\n\t; Assign Instruction\n");

        Element dest = instruction.getDest(); // instruction type <=> inst.getTypeOfAssign()
        Instruction rhs = instruction.getRhs();
        InstructionType instType = rhs.getInstType();

        if (instType.equals(InstructionType.BINARYOPER)) {
            addBinaryOperation(codeBuilder, (BinaryOpInstruction) rhs); // Add binary operation loads and calculation
            codeBuilder.append("\t");
        } else if (instType.equals(InstructionType.NOPER)) {
            SingleOpInstruction op = (SingleOpInstruction) rhs;
            codeBuilder.append("\t");
            loadElement(codeBuilder, op.getSingleOperand());
        } else if (instType.equals(InstructionType.GETFIELD)) {
            codeBuilder.append(addInstruction(rhs));
        } else {
            // Unary operation
            UnaryOpInstruction op = (UnaryOpInstruction) rhs;
            addUnaryOperation(codeBuilder, op);
            codeBuilder.append("\t");
        }

        // Storing final value in variable
        storeElement(codeBuilder, dest, rhs);

        codeBuilder.append("\n\t; End Assign Instruction\n\n");

        return codeBuilder.toString();
    }

    private void addUnaryOperation(StringBuilder codeBuilder, UnaryOpInstruction op) {
        codeBuilder.append("\n\t; Executing unary operation\n\t");

        // Load operand if needed to execute unary operation
        loadElement(codeBuilder, op.getOperand());


        // Execute unary operation
        if (op.getOperation().getOpType().equals(OperationType.NOTB)) {
            codeBuilder.append("iconst_1\n");
            codeBuilder.append("\tixor\n");
        } else if (op.getOperation().getOpType().equals(OperationType.SUB)) {
            codeBuilder.append("ineg\n");
        }

        codeBuilder.append("\t; End unary operation\n\n");

    }

    private void storeElement(StringBuilder codeBuilder, Element dest, Instruction rhs) {
        if (!(rhs instanceof CallInstruction)) {
            Operand operand = (Operand) dest;
            if (operand.getType().getTypeOfElement().equals(ElementType.INT32) || operand.getType().getTypeOfElement().equals(ElementType.BOOLEAN))
                codeBuilder.append("i"); // Number
            else codeBuilder.append("a"); // Generic Object
            codeBuilder.append("store ");
        } else {
            codeBuilder.append("astore ");
        }
        String name = ((Operand) dest).getName();
        codeBuilder.append(getRegister(name)).append(" ; ").append(name);
    }

    private void addBinaryOperation(StringBuilder codeBuilder, BinaryOpInstruction opInstruction) {

        OperationType opType = opInstruction.getOperation().getOpType();

        codeBuilder.append("\n\t; Executing binary operation\n\t");

        // Load operands
        loadElement(codeBuilder, opInstruction.getLeftOperand());
        loadElement(codeBuilder, opInstruction.getRightOperand());

        // execute operation
        BinaryOpInstAux(codeBuilder, opType);

        codeBuilder.append("\t; End binary operation\n\n ");
    }

    private void BinaryOpInstAux(StringBuilder codeBuilder, OperationType opType) {

        switch (opType) {
            case ADD -> codeBuilder.append("iadd\n");
            case SUB -> codeBuilder.append("isub\n");
            case MUL -> codeBuilder.append("imul\n");
            case DIV -> codeBuilder.append("idiv\n");
            case LTH -> {
                String trueL = getNewLabel(), endL = getNewLabel();
                codeBuilder.append("if_icmplt ").append(trueL).append("\n");
                codeBuilder.append("\ticonst_0\n"); // false scope
                codeBuilder.append("\tgoto ").append(endL).append("\n");
                codeBuilder.append(trueL).append(":\n");
                codeBuilder.append("\ticonst_1\n"); // true scope
                codeBuilder.append(endL).append(":\n");
            }
            case AND -> codeBuilder.append("iand\n");
            default -> System.out.println("Binary op error");
        }
    }

    private String addInstruction(Instruction instruction) {

        StringBuilder codeBuilder = new StringBuilder();
        String instType = instruction.getInstType().toString();

        switch (instType) {
            case "ASSIGN" -> {
                return addInstructionAssign((AssignInstruction) instruction);
            }
            case "RETURN" -> {
                ReturnInstruction inst = (ReturnInstruction) instruction;
                codeBuilder.append("\t");

                loadElement(codeBuilder, inst.getOperand());

                switch (inst.getReturnType().toString()) {
                    case "VOID" -> codeBuilder.append("return");
                    case "INT32", "BOOLEAN" -> codeBuilder.append("ireturn");
                    default -> codeBuilder.append("areturn"); // ARRAYREF or OBJECTREF
                }

                return codeBuilder.toString();
            }
            case "CALL" -> {
                return addCallInstruction((CallInstruction) instruction);
            }
            case "GETFIELD", "PUTFIELD" -> {
                addGetPutField(codeBuilder, instruction);
                return codeBuilder.toString();
            }
            case "UNARYOPER" -> {
                System.out.println("HERE:" + instruction.getInstType());
                addUnaryOperation(codeBuilder, (UnaryOpInstruction) instruction);
                return codeBuilder.toString();
            }
            case "BINARYOPER" -> {
                addBinaryOperation(codeBuilder, (BinaryOpInstruction) instruction);
                return codeBuilder.toString();
            }
            case "BRANCH" -> {
                addConditionalBranch(codeBuilder, (OpCondInstruction) instruction);
                return codeBuilder.toString();
            }
            case "GOTO" -> {
                GotoInstruction inst = (GotoInstruction) instruction;
                return "\n\tgoto " + inst.getLabel() + "\n";
            }
            default -> {
                return "error";
            }
        }

        // 		invokevirtual(c.Foo,"test",$1.A.array.classArray).V;
        // CALL Operand: c OBJECTREF, Literal: "test", Operand: A ARRAYREF

    }

    private void addGetPutField(StringBuilder codeBuilder, Instruction instruction) {

        Operand op1, op2;
        String typeClass = "";

        // getfiled ClassName/fieldName Type
        // putfield ClassName/fieldName Type

        if (instruction instanceof GetFieldInstruction inst) {
            op1 = (Operand) (inst).getFirstOperand();
            op2 = (Operand) (inst).getSecondOperand();
            typeClass = ((ClassType) op1.getType()).getName();

            codeBuilder.append("\t");
            loadElement(codeBuilder, op1);
            codeBuilder.append("getfield").append(" ").append(typeClass).append("/");
            codeBuilder.append(op2.getName()).append(" ").append(toJasminType(op2.getType().toString()));
            codeBuilder.append("\n\t");
            return;
        }

        PutFieldInstruction inst = (PutFieldInstruction) instruction;

        op1 = (Operand) (inst).getFirstOperand(); // class name
        op2 = (Operand) (inst).getSecondOperand(); // field name where value will be stored
        Element op3 = (inst).getThirdOperand(); // value to be stored
        typeClass = ((ClassType) op1.getType()).getName();

        codeBuilder.append("\t");
        loadElement(codeBuilder, op1);
        loadElement(codeBuilder, op3);
        codeBuilder.append("putfield").append(" ").append(typeClass).append("/");
        codeBuilder.append(op2.getName()).append(" ").append(toJasminType(op3.getType().toString()));
        codeBuilder.append("\n\t");

    }

    private void addConditionalBranch(StringBuilder codeBuilder, OpCondInstruction instruction) {
        OpInstruction opType = instruction.getCondition();
        String trueL = getNewLabel(); // should I use instruction.getLabel() or my getNewLabel() like the line below?
        String endL = getNewLabel();

        codeBuilder.append("\n\t; Executing conditional branch\n\t");

        Element leftOperand = opType.getOperands().get(0);
        Element rightOperand = opType.getOperands().get(1);

        // Load operands if needed to execute binary operation
        loadElement(codeBuilder, leftOperand);
        loadElement(codeBuilder, rightOperand);

        // execute operation (only LTH is supported)
        codeBuilder.append("if_icmplt ").append(trueL).append("\n"); // if condition is true, jump to true label

        // TODO : CODE IF CONDITION IS FALSE HERE

        // if condition is false, jump to end label
        codeBuilder.append("\tgoto ").append(endL).append("\n");

        codeBuilder.append(trueL).append(":\n");

        // TODO : CODE IF CONDITION IS TRUE HERE

        // end label
        codeBuilder.append(endL).append(":\n");

        codeBuilder.append("\t; End of conditional branch");
    }

    private String addCallInstruction(CallInstruction inst) {

        // load arguments
        StringBuilder codeBuilder = new StringBuilder();
        codeBuilder.append("\n\t; Making a call instruction\n\t");

        String name = ((Operand) inst.getFirstArg()).getName();
        String method = ((LiteralElement) inst.getSecondArg()).getLiteral();

        // hardcoded println
        if (name.equals("io") && method.equals("\"println\"")) {
            codeBuilder.append("getstatic java/lang/System/out Ljava/io/PrintStream;\n\t");
            loadElement(codeBuilder, inst.getListOfOperands().get(0));
            codeBuilder.append("invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V");
            codeBuilder.append("\n\t; End of call instruction\n");
            return codeBuilder.toString();
        } else {
            System.out.println("Call instruction not supported");
            System.out.println("NAME: " + name + " method: " + method);
        }


        // 	getstatic java/lang/System/out Ljava/io/PrintStream;
        //	ldc "Program finished"
        //	invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V

        // load other arguments
        for (Element arg : inst.getListOfOperands())
            loadElement(codeBuilder, arg);

        method = method.substring(1, method.length() - 1); // remove quotes from method name

        // invoke method
        codeBuilder.append("invokevirtual ").append(name);
        if (inst.getSecondArg() != null) codeBuilder.append(".").append(method);
        codeBuilder.append("(");

        for (Element arg : inst.getListOfOperands())
            codeBuilder.append(toJasminType(arg.getType().toString()));

        codeBuilder.append(")").append(toJasminType(inst.getReturnType().toString())).append("\n");
        codeBuilder.append("\t; End of call instruction\n");
        return codeBuilder.toString();
    }

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        this.classe = ollirResult.getOllirClass();
        //this.showClass(); // debug print class

        code += this.addHeaders();
        code += "; Imports\n";
        code += this.addImports();
        code += "\n; Fields\n";
        code += this.addFields();
        code += "\n; Constructor";
        code += this.addConstructor(); // TODO: how to handle multiple constructors? --> they dont exist in ollir?
        code += this.addMethods();

        System.out.println("\n======================JASMIN CODE======================\n");
        System.out.println(code);
        System.out.println("======================================================");


        return new JasminResult(code);
    }

}

