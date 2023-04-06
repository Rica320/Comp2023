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

    Method currentMethod;

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

        if (element instanceof ArrayOperand) {
            arrayAccess(codeBuilder, element);
            return;
        }

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
            } else {
                Operand variable = (Operand) element;
                String name = variable.getName();
                codeBuilder.append("iload ").append(getRegister(name)).append(" ; ").append(name);
                codeBuilder.append("\n\t");
                return;
            }
        } else if (element.isLiteral()) { // string
            codeBuilder.append("ldc ").append(((LiteralElement) element).getLiteral());
            codeBuilder.append("\n\t");
            return;
        }

        // array or object reference
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
        // TODO: se classe n existir, crasha o jasmin
        // if (this.classe.getSuperClass() != null) code += ".super " + this.classe.getSuperClass() + "\n";
        //else {
        code += ".super java/lang/Object\n";
        //}
        code += "\n";
        return code;
    }

    private String addImports() {
        StringBuilder imports = new StringBuilder();

        if (this.classe.getImports().size() == 0) return "; No imports\n";

        for (String imp : this.classe.getImports()) {
            imports.append("; .import ").append(imp).append("\n"); // TODO: imports are broken? gpt suggested ldc instead of .import
        }
        return imports.toString();
    }

    public String addFields() {
        StringBuilder codeBuilder = new StringBuilder();
        if (this.classe.getFields().size() == 0) return "; No fields\n";
        this.classe.getFields().forEach(field -> codeBuilder.append(".field public ").append(field.getFieldName()).append(" ").append(toJasminType(field.getFieldType().toString())).append("\n"));
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
            this.currentMethod = method;


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

        if (dest instanceof ArrayOperand) {
            // arr, index, value, iastore

            codeBuilder.append("\t; Start Array assign\n\t");

            // load array (cant use loadElement because IDK)
            Operand variable = (Operand) dest;
            String name = variable.getName();
            codeBuilder.append("aload ").append(getRegister(name)).append(" ; ").append(name);
            codeBuilder.append("\n\t");

            // get index
            loadElement(codeBuilder, ((ArrayOperand) dest).getIndexOperands().get(0));
            codeBuilder.deleteCharAt(codeBuilder.length() - 1); // delete last character (new line)

            // value will be calculated below by the rhs instruction
        }

        if (instType.equals(InstructionType.BINARYOPER)) {
            addBinaryOperation(codeBuilder, (BinaryOpInstruction) rhs); // Add binary operation loads and calculation
            codeBuilder.append("\t");
        } else if (instType.equals(InstructionType.NOPER)) {
            SingleOpInstruction op = (SingleOpInstruction) rhs;

            if (op.getSingleOperand() instanceof ArrayOperand) {
                arrayAccess(codeBuilder, op.getSingleOperand());
            } else {
                codeBuilder.append("\t");
                loadElement(codeBuilder, op.getSingleOperand());
            }

        } else if (instType.equals(InstructionType.GETFIELD)) {
            addGetPutField(codeBuilder, rhs);
        } else if (instType.equals(InstructionType.CALL)) {
            codeBuilder.append(addCallInstruction((CallInstruction) rhs));
        } else {
            // Unary operation
            UnaryOpInstruction op = (UnaryOpInstruction) rhs;
            addUnaryOperation(codeBuilder, op);
            codeBuilder.append("\t");
        }

        // Storing final value in variable
        if (dest instanceof ArrayOperand) codeBuilder.append("iastore\n\t; End Array Assign\n\t");
        else storeElement(codeBuilder, dest, rhs);

        codeBuilder.append("\n\t; End Assign Instruction\n\n");

        return codeBuilder.toString();
    }

    public void arrayAccess(StringBuilder codeBuilder, Element elem) {
        codeBuilder.append("\t; Start Array access\n\t");

        Operand variable = (Operand) elem;
        String name = variable.getName();
        codeBuilder.append("aload ").append(getRegister(name)).append(" ; ").append(name); // load array
        codeBuilder.append("\n\t");

        loadElement(codeBuilder, ((ArrayOperand) elem).getIndexOperands().get(0)); // load index
        codeBuilder.append("iaload\n\t");
        codeBuilder.append("; End Array access\n\t");
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

        Operand operand = (Operand) dest;
        if (operand.getType().getTypeOfElement().equals(ElementType.INT32) || operand.getType().getTypeOfElement().equals(ElementType.BOOLEAN))
            codeBuilder.append("i"); // Number
        else codeBuilder.append("a"); // Generic Object
        codeBuilder.append("store ");

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
                codeBuilder.append("; Making lth operation\n\t");
                codeBuilder.append("if_icmplt ").append(trueL).append("\n");
                codeBuilder.append("\ticonst_0\n"); // false scope
                codeBuilder.append("\tgoto ").append(endL).append("\n");
                codeBuilder.append(trueL).append(":\n");
                codeBuilder.append("\ticonst_1\n"); // true scope
                codeBuilder.append(endL).append(":\n");
                codeBuilder.append("; End lth operation\n");
            }
            case AND -> codeBuilder.append("iand\n");
            default -> System.out.println("Binary op error");
        }
    }

    private String addInstruction(Instruction instruction) {

        StringBuilder codeBuilder = new StringBuilder();
        String instType = instruction.getInstType().toString();

        // add label to instruction if needed
        var labels = currentMethod.getLabels(instruction);

        for (String label : labels)
            codeBuilder.append(label).append(":\n");


        switch (instType) {
            case "ASSIGN" -> {
                return codeBuilder + addInstructionAssign((AssignInstruction) instruction);
            }
            case "RETURN" -> {
                ReturnInstruction inst = (ReturnInstruction) instruction;
                codeBuilder.append("\t");

                if (inst.getReturnType().toString().equals("VOID")) {
                    codeBuilder.append("return");
                    return codeBuilder.toString();
                }


                loadElement(codeBuilder, inst.getOperand());

                switch (inst.getReturnType().toString()) {
                    case "INT32", "BOOLEAN" -> codeBuilder.append("ireturn");
                    default -> codeBuilder.append("areturn"); // ARRAYREF or OBJECTREF
                }

                return codeBuilder.toString();
            }
            case "CALL" -> {
                return codeBuilder + addCallInstruction((CallInstruction) instruction);
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
                if (instruction instanceof OpCondInstruction inst) {
                    addConditionalBranch(codeBuilder, (OpCondInstruction) instruction);
                } else if (instruction instanceof SingleOpCondInstruction inst) {
                    addSingleConditionalBranch(codeBuilder, (SingleOpCondInstruction) instruction);
                } else System.out.println("Error in branch instruction");
                return codeBuilder.toString();
            }
            case "GOTO" -> {
                GotoInstruction inst = (GotoInstruction) instruction;
                return codeBuilder + "\n\tgoto " + inst.getLabel() + "\n";
            }
            default -> {
                return "error";
            }
        }

    }


    private void addGetPutField(StringBuilder codeBuilder, Instruction instruction) {

        Operand op1, op2;
        String typeClass = "";      // getfiled ClassName/fieldName Type
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
        String label = instruction.getLabel();

        codeBuilder.append("\n\t; Executing conditional branch\n\t");

        if (opType.getOperands().size() != 2) {

            addUnaryOperation(codeBuilder, (UnaryOpInstruction) opType);
            codeBuilder.append("ifne ").append(label).append("\n");

        } else {
            Element leftOperand = opType.getOperands().get(0);
            Element rightOperand = opType.getOperands().get(1);

            // Load operands if needed to execute binary operation
            loadElement(codeBuilder, leftOperand);
            loadElement(codeBuilder, rightOperand);

            codeBuilder.append("if_icmplt ").append(label).append("\n");
        }

        codeBuilder.append("\t; End of conditional branch");
    }


    private void addSingleConditionalBranch(StringBuilder codeBuilder, SingleOpCondInstruction instruction) {
        Operand op = (Operand) instruction.getCondition().getSingleOperand();
        String label = instruction.getLabel();

        codeBuilder.append("\n\t; Executing conditional branch\n\t");

        loadElement(codeBuilder, op);

        codeBuilder.append("ifne ").append(label).append("\n");

        codeBuilder.append("\t; End conditional branch\n\n");
    }

    private String addCallInstruction(CallInstruction inst) {

        // load arguments
        StringBuilder codeBuilder = new StringBuilder();
        codeBuilder.append("\t; Making a call instruction\n\t");


        var callType = (inst.getInvocationType()).toString();
        switch (callType) {
            case "NEW" -> callNew(codeBuilder, inst);
            case "invokevirtual" -> callInvokeVirtual(codeBuilder, inst);
            case "invokestatic" -> callInvokeStatic(codeBuilder, inst);
            case "invokespecial" -> callInvokeSpecial(codeBuilder, inst);
            case "ldc" -> callLDC(codeBuilder, inst);
            case "arraylength" -> { // t1.i32 :=.i32 arraylength($1.A.array.i32).i32;
                // perguntar ao ricardo cm funciona
                codeBuilder.append("arraylength"); // TODO should i store the result in a local variable?
            }
            default -> {
                System.out.println("Call instruction not supported");
                System.out.println("NAME: " + callType);
            }
        }

        codeBuilder.append("\n\t; End of call instruction\n\t");
        return codeBuilder.toString();
    }


    public void callNew(StringBuilder codeBuilder, CallInstruction inst) {
        var FirstName = inst.getFirstArg().getType();

        if (FirstName instanceof ArrayType arr) { // New array

            loadElement(codeBuilder, inst.getListOfOperands().get(0)); // load array size

            codeBuilder.append("newarray int"); // we only have arrays of integers

            codeBuilder.append("\n\t");
            return;
        }

        // New Object
        codeBuilder.append("new ").append(((ClassType) FirstName).getName()).append("\n\t");
        codeBuilder.append("dup\n\t");
        codeBuilder.append("invokespecial ").append(((ClassType) inst.getFirstArg().getType()).getName()).append("/<init>()V");
    }

    public void callInvokeVirtual(StringBuilder codeBuilder, CallInstruction inst) {

        // is this only needed to call self methods?

        String name = ((ClassType) inst.getFirstArg().getType()).getName();
        String method = ((LiteralElement) inst.getSecondArg()).getLiteral();

        addNewObject(codeBuilder, inst); // load object to call method on

        // load arguments
        for (Element arg : inst.getListOfOperands())
            loadElement(codeBuilder, arg);

        // invoke method
        codeBuilder.append("invokevirtual ").append(name); // TODO: HERE THE NAME IS WRONG! SHOULD BE CLASS NAME?
        invokeArgs(codeBuilder, inst, method);
    }

    private void addNewObject(StringBuilder codeBuilder, CallInstruction inst) {
        codeBuilder.append("\n\t; Creating new object\n\t");
        codeBuilder.append("new ").append(((ClassType) inst.getFirstArg().getType()).getName()).append("\n\t");
        codeBuilder.append("dup\n\t");
        codeBuilder.append("invokespecial ").append(((ClassType) inst.getFirstArg().getType()).getName()).append("/<init>()V\n\t");
        codeBuilder.append("; End of creating new object\n\n\t");
    }

    public void callInvokeStatic(StringBuilder codeBuilder, CallInstruction inst) {
        String name = ((Operand) inst.getFirstArg()).getName();
        String method = ((LiteralElement) inst.getSecondArg()).getLiteral();

        // load arguments
        for (Element arg : inst.getListOfOperands())
            loadElement(codeBuilder, arg);

        // invoke method
        codeBuilder.append("invokestatic ").append(name);
        invokeArgs(codeBuilder, inst, method);

    }

    private void callInvokeSpecial(StringBuilder codeBuilder, CallInstruction inst) {
        String method = ((LiteralElement) inst.getSecondArg()).getLiteral();

        method = method.substring(1, method.length() - 1); // remove quotes from method name

        codeBuilder.append("invokespecial ").append(method).append("(");

        for (Element arg : inst.getListOfOperands()) {
            codeBuilder.append(toJasminType(arg.getType().toString()));
        }
        codeBuilder.append(")");
        codeBuilder.append(toJasminType(inst.getReturnType().toString()));

        // TODO should i force a store here?
    }

    private void invokeArgs(StringBuilder codeBuilder, CallInstruction inst, String method) {

        method = method.substring(1, method.length() - 1); // remove quotes from method name

        if (inst.getSecondArg() != null) codeBuilder.append(".").append(method);
        codeBuilder.append("(");

        for (Element arg : inst.getListOfOperands()) // add arguments types
            codeBuilder.append(toJasminType(arg.getType().toString()));

        codeBuilder.append(")").append(toJasminType(inst.getReturnType().toString()));
    }

    public void callLDC(StringBuilder codeBuilder, CallInstruction inst) {
        codeBuilder.append("ldc ").append(((LiteralElement) inst.getFirstArg()).getLiteral()); // todo: check if this is correct
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

