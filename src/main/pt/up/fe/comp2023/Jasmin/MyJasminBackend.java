package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.SymbolTable.MySymbolTable;

// • Assignments
//• Arithmetic operations (with correct precedence)
//• Method invocation

public class MyJasminBackend implements JasminBackend {

    ClassUnit classe;
    String code = "";
    MySymbolTable st;

    int labelCounter = 0;

    public MyJasminBackend(MySymbolTable st) {
        this.st = st;
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

            if (method.getMethodName().equals("main"))
                codeBuilder.append("\n.method public static main([Ljava/lang/String;)V\n");
            else if (method.getMethodName().equals(this.classe.getClassName())) return; // ignore constructor
            else codeBuilder.append("\n.method public ").append(method.getMethodName()).append("(");

            method.getParams().forEach(param -> codeBuilder.append(toJasminType(param.getType().toString())));

            String returnType = method.getReturnType().toString();
            codeBuilder.append(")").append(toJasminType(returnType)).append("\n");

            // in this phase we don't need to worry about locals and stack limits
            codeBuilder.append("\t.limit locals 99;\n");
            codeBuilder.append("\t.limit stack 99;\n\n");

            // add instructions
            method.getInstructions().forEach(instruction -> codeBuilder.append(addInstruction(instruction)).append("\n"));

            codeBuilder.append(".end method\n\n");
        });
        return codeBuilder.toString();
    }

    private String addReturn(String returnType) {
        switch (returnType) {
            case "VOID" -> {
                return "\treturn";
            }
            case "INT32", "BOOLEAN" -> {
                return "\tireturn";
            }
            default -> {
                return "\tareturn"; // ARRAYREF or OBJECTREF
            }
        }
    }

    private String addInstructionAssign(AssignInstruction instruction) {
        StringBuilder codeBuilder = new StringBuilder();

        codeBuilder.append("\n\t; Assign Instruction\n");

        Element dest = instruction.getDest(); // instruction type <=> inst.getTypeOfAssign()
        Instruction rhs = instruction.getRhs();
        InstructionType instType = rhs.getInstType();

        if (instType.equals(InstructionType.BINARYOPER)) {
            addBinaryOperation(codeBuilder, (BinaryOpInstruction) rhs); // Add binary operation loads and calculation
        } else if (instType.equals(InstructionType.NOPER)) {
            SingleOpInstruction op = (SingleOpInstruction) rhs;

            boolean isNumber = op.getSingleOperand().getType().getTypeOfElement().equals(ElementType.INT32) || op.getSingleOperand().getType().getTypeOfElement().equals(ElementType.BOOLEAN);
            codeBuilder.append("\t");

            if (op.getSingleOperand().isLiteral()) // Number or Boolean
                codeBuilder.append("bipush ").append(((LiteralElement) op.getSingleOperand()).getLiteral());
            else // Other types
                codeBuilder.append("aload_").append(((Operand) op.getSingleOperand()).getName());

            codeBuilder.append("\n");
        } else {
            // Unary operation
            UnaryOpInstruction op = (UnaryOpInstruction) rhs;
            addUnaryOperation(codeBuilder, op);
        }

        // Storing final value in variable
        addStore(codeBuilder, dest, rhs);

        codeBuilder.append("\n\t; End Assign Instruction\n\n");

        return codeBuilder.toString();
    }

    private void addUnaryOperation(StringBuilder codeBuilder, UnaryOpInstruction op) {
        codeBuilder.append("\n\t; Executing unary operation\n");

        // Load operand if needed to execute unary operation
        if (op.getOperand().isLiteral()) {
            codeBuilder.append("\t");
            if (op.getOperand().getType().getTypeOfElement().equals(ElementType.INT32) || op.getOperand().getType().getTypeOfElement().equals(ElementType.BOOLEAN))
                codeBuilder.append("bipush ").append(((LiteralElement) op.getOperand()).getLiteral());
            else codeBuilder.append("aload_").append(((Operand) op.getOperand()).getName());
            codeBuilder.append("\n");
        } else if (op.getOperand().getType().getTypeOfElement().equals(ElementType.BOOLEAN)) {
            String boolVal = ((Operand) op.getOperand()).getName();
            int value = boolVal.equals("true") ? 1 : 0;
            codeBuilder.append("\tbipush ").append(value).append(" ; ").append(boolVal).append("\n");
        }

        // Execute unary operation
        codeBuilder.append("\t");
        if (op.getOperation().getOpType().equals(OperationType.NOTB)) {
            codeBuilder.append("iconst_1\n");
            codeBuilder.append("\tixor\n");
        } else if (op.getOperation().getOpType().equals(OperationType.SUB)) {
            codeBuilder.append("ineg\n");
        }

        codeBuilder.append("\t; End unary operation\n\n");

    }

    private void addStore(StringBuilder codeBuilder, Element dest, Instruction rhs) {
        codeBuilder.append("\t");
        if (!(rhs instanceof CallInstruction)) {
            Operand operand = (Operand) dest;
            if (operand.getType().getTypeOfElement().equals(ElementType.INT32) || operand.getType().getTypeOfElement().equals(ElementType.BOOLEAN))
                codeBuilder.append("i"); // Number
            else codeBuilder.append("a"); // Generic Object
            codeBuilder.append("store_");
            codeBuilder.append(((Operand) dest).getName());
        } else {
            codeBuilder.append("astore_");
            codeBuilder.append(((Operand) dest).getName());
        }
    }

    private void addBinaryOperation(StringBuilder codeBuilder, BinaryOpInstruction opInstruction) {

        OperationType opType = opInstruction.getOperation().getOpType();

        codeBuilder.append("\n\t; Executing binary operation\n");

        // Load operands if needed to execute binary operation

        boolean isLeftLiteral = opInstruction.getLeftOperand().isLiteral();
        boolean isRightLiteral = opInstruction.getRightOperand().isLiteral();

        if (isLeftLiteral || isRightLiteral) {
            // if one operand is literal and the other a variable, we need to load the variable operand first
            codeBuilder.append("\t");
            if (isLeftLiteral) {
                codeBuilder.append("bipush ").append(((LiteralElement) opInstruction.getLeftOperand()).getLiteral()).append("\n"); // load literal
            } else { // right operand is literal
                codeBuilder.append("aload_").append(((Operand) opInstruction.getLeftOperand()).getName()).append("\n"); // load variable
            }
            codeBuilder.append("\t");
            if (isRightLiteral) {
                codeBuilder.append("bipush ").append(((LiteralElement) opInstruction.getRightOperand()).getLiteral()).append("\n");
            } else { // left operand is literal
                codeBuilder.append("aload_").append(((Operand) opInstruction.getRightOperand()).getName()).append("\n");
            }
        } else { // both operands are variables
            // load both operands
            codeBuilder.append("\t");
            codeBuilder.append("aload_").append(((Operand) opInstruction.getLeftOperand()).getName()).append("\n");
            codeBuilder.append("\t");
            codeBuilder.append("aload_").append(((Operand) opInstruction.getRightOperand()).getName()).append("\n");
        }

        // execute operation
        BinaryOpInstAux(codeBuilder, opType);

        codeBuilder.append("\t; End binary operation\n\n ");
    }

    private void BinaryOpInstAux(StringBuilder codeBuilder, OperationType opType) {
        codeBuilder.append("\t");
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

                if (inst.getOperand() != null) {
                    if (inst.getOperand().isLiteral()) { // int or boolean
                        LiteralElement literal = (LiteralElement) inst.getOperand();
                        return "\n\ticonst_" + literal.getLiteral() + "\n" + addReturn(inst.getReturnType().toString());
                    } else { // Object
                        Operand operand = (Operand) inst.getOperand();
                        return "\n\taload_" + operand.getName() + "\n" + addReturn(inst.getReturnType().toString());
                    }
                }
                // Else return void
                return "\n" + addReturn(inst.getReturnType().toString());
            }
            case "CALL" -> {
                return addCallInstruction((CallInstruction) instruction);
            }
            case "GETFIELD" -> {
                //GetFieldInstruction inst = (GetFieldInstruction) instruction;
                return "getfield";
            }
            case "PUTFIELD" -> {
                //PutFieldInstruction inst = (PutFieldInstruction) instruction;
                return "putfield";
            }
            case "UNARYOPER" -> {
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

    private void addConditionalBranch(StringBuilder codeBuilder, OpCondInstruction instruction) {
        OpInstruction opType = instruction.getCondition();
        String trueL = getNewLabel(); // should I use instruction.getLabel() or my getNewLabel() like the line below?
        String endL = getNewLabel();

        codeBuilder.append("\n\t; Executing conditional branch\n");

        Element leftOperand = opType.getOperands().get(0);
        Element rightOperand = opType.getOperands().get(1);

        // Load operands if needed to execute binary operation
        boolean isLeftLiteral = leftOperand.isLiteral();
        boolean isRightLiteral = rightOperand.isLiteral();

        if (isLeftLiteral || isRightLiteral) {
            // if one operand is literal and the other a variable, we need to load the variable operand first
            codeBuilder.append("\t");
            if (isLeftLiteral) {
                codeBuilder.append("bipush ").append(((LiteralElement) leftOperand).getLiteral()).append("\n"); // load literal
            } else { // right operand is literal
                codeBuilder.append("aload_").append(((Operand) leftOperand).getName()).append("\n"); // load variable
            }
            codeBuilder.append("\t");
            if (isRightLiteral) {
                codeBuilder.append("bipush ").append(((LiteralElement) rightOperand).getLiteral()).append("\n");
            } else { // left operand is literal
                codeBuilder.append("aload_").append(((Operand) rightOperand).getName()).append("\n");
            }
        } else { // both operands are variables
            // load both operands
            codeBuilder.append("\t");
            codeBuilder.append("aload_").append(((Operand) leftOperand).getName()).append("\n");
            codeBuilder.append("\t");
            codeBuilder.append("aload_").append(((Operand) rightOperand).getName()).append("\n");
        }

        // execute operation (only LTH is supported)
        codeBuilder.append("\tif_icmplt ").append(trueL).append("\n"); // if condition is true, jump to true label

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
        if (inst.getFirstArg() != null) {
            if (inst.getFirstArg().isLiteral())
                codeBuilder.append("ldc ").append(((LiteralElement) inst.getFirstArg()).getLiteral()).append("\n");
            else
                codeBuilder.append("aload_").append(((Operand) inst.getFirstArg()).getName()).append("\n"); // should be a number

            if (inst.getSecondArg() != null) {
                if (inst.getSecondArg().isLiteral())
                    codeBuilder.append("\tldc ").append(((LiteralElement) inst.getSecondArg()).getLiteral()).append("\n");
                else
                    codeBuilder.append("\taload_").append(((Operand) inst.getSecondArg()).getName()).append("\n"); // should be a number
            }
        }

        for (Element arg : inst.getListOfOperands()) {
            if (arg.isLiteral())
                codeBuilder.append("\tbipush ").append(((LiteralElement) arg).getLiteral()).append("\n");
            else codeBuilder.append("\taload_").append(((Operand) arg).getName()).append("\n"); // should be a number
        }

        // invoke method
        codeBuilder.append("\tinvokevirtual " + "<METHOD_NAME>(");

        if (inst.getFirstArg() != null) {
            codeBuilder.append(toJasminType(inst.getFirstArg().getType().toString()));
            if (inst.getSecondArg() != null) codeBuilder.append(toJasminType(inst.getSecondArg().getType().toString()));
        }

        for (Element arg : inst.getListOfOperands())
            codeBuilder.append(toJasminType(arg.getType().toString()));

        codeBuilder.append(")").append(toJasminType(inst.getReturnType().toString())).append("\n");
        codeBuilder.append("\t; End of call instruction\n");
        return codeBuilder.toString();
    }

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        this.classe = ollirResult.getOllirClass();
        this.showClass(); // debug print class

        code += this.addHeaders();
        code += "; Imports\n";
        code += this.addImports();
        code += "\n; Fields\n";
        code += this.addFields();
        code += "\n; Constructor";
        code += this.addConstructor();
        code += "\n; ================ Methods ================\n";
        code += this.addMethods();

        System.out.println("\n======================JASMIN CODE======================\n");
        System.out.println(code);
        System.out.println("======================================================");


        return new JasminResult(code);
    }

}
