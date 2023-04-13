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
    StringBuilder code = new StringBuilder();

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


    private void loadElement(Element element) {
        boolean isNumber = element.getType().getTypeOfElement().equals(ElementType.INT32);
        boolean isBoolean = element.getType().getTypeOfElement().equals(ElementType.BOOLEAN);

        if (element instanceof ArrayOperand) {
            arrayAccess(element);
            return;
        }

        if (isNumber || isBoolean) {
            if (element.isLiteral()) {
                LiteralElement literal = (LiteralElement) element;
                int value = Integer.parseInt(literal.getLiteral());
                if (value < 6) code.append("iconst_");  // more efficient than bipush
                else if (value < 128) code.append("bipush ");
                else code.append("ldc ");
                code.append(value);
            } else {
                Operand variable = (Operand) element;
                String name = variable.getName();
                code.append("iload ").append(getRegister(name)).append(" ; ").append(name);
            }
            code.append("\n\t");
            return;
        } else if (element.isLiteral()) { // string
            code.append("ldc ").append(((LiteralElement) element).getLiteral());
            code.append("\n\t");
            return;
        }

        // array or object reference
        Operand variable = (Operand) element;
        String name = variable.getName();
        code.append("aload ").append(getRegister(name)).append(" ; ").append(name);
        code.append("\n\t");

    }

    private void addHeaders() {
        code.append(".class public ").append(this.classe.getClassName()).append("\n");
        if (this.classe.getSuperClass() != null)
            code.append(".super ").append(this.classe.getSuperClass()).append("\n");
        else code.append(".super java/lang/Object\n");

    }

    public void addFields() {
        code.append("\n; Fields\n");
        if (this.classe.getFields().size() == 0) {
            code.append("; No fields\n");
            return;
        }
        this.classe.getFields().forEach(field -> {
            code.append(".field public ");
            code.append(field.getFieldName()).append(" ").append(toJasminType(field.getFieldType().toString())).append("\n");
        });
    }

    public void addConstructor() {
        code.append("\n; Constructor");
        code.append("\n.method public <init>()V");
        code.append("\n\taload_0");
        code.append("\n\tinvokespecial ").append(this.classe.getSuperClass() == null ? "java/lang/Object" : this.classe.getSuperClass()).append("/<init>()V");
        code.append("\n\treturn");
        code.append("\n.end method\n\n");
    }

    private void addMethods() {
        this.classe.getMethods().forEach(method -> {

            currVarTable = method.getVarTable();
            this.currentMethod = method;

            if (method.getMethodName().equals("main"))
                code.append("\n\n; main method\n.method public static main([Ljava/lang/String;)V\n");
            else if (method.getMethodName().equals(this.classe.getClassName())) return; // ignore constructor
            else code.append("\n.method public ").append(method.getMethodName()).append("(");

            if (!method.getMethodName().equals("main"))
                method.getParams().forEach(param -> code.append(toJasminType(param.getType().toString())));

            if (!method.getMethodName().equals("main")) { // ignore constructor because its already defined
                String returnType = method.getReturnType().toString(); // TODO: Perguntar ao prof se LSimple --> casting é suposto ?

                code.append(")").append(toJasminType(returnType)).append("\n");
            }

            // in this phase we don't need to worry about locals and stack limits
            code.append("\t.limit stack 99\n");
            code.append("\t.limit locals 99\n\n");

            // add instructions
            method.getInstructions().forEach(instruction -> {
                addInstruction(instruction);
                code.append("\n");
            });

            currVarTable = null;

            code.append(".end method\n\n");
        });
    }


    private void addInstructionAssign(AssignInstruction instruction) {
        code.append("\n\t; Assign Instruction\n");

        Element dest = instruction.getDest(); // instruction type <=> inst.getTypeOfAssign()
        Instruction rhs = instruction.getRhs();
        InstructionType instType = rhs.getInstType();

        if (dest instanceof ArrayOperand) {

            code.append("\t; Start Array assign\n\t");

            // load array (cant use loadElement because IDK)
            Operand variable = (Operand) dest;
            String name = variable.getName();
            code.append("aload ").append(getRegister(name)).append(" ; ").append(name);
            code.append("\n\t");

            // get index
            loadElement(((ArrayOperand) dest).getIndexOperands().get(0));
            code.deleteCharAt(code.length() - 1); // delete last character (new line)

            // value will be calculated below by the rhs instruction
        }

        if (instType.equals(InstructionType.BINARYOPER)) {
            addBinaryOperation((BinaryOpInstruction) rhs); // Add binary operation loads and calculation
            code.append("\t");
        } else if (instType.equals(InstructionType.NOPER)) {
            SingleOpInstruction op = (SingleOpInstruction) rhs;

            if (op.getSingleOperand() instanceof ArrayOperand) {
                arrayAccess(op.getSingleOperand());
            } else {
                code.append("\t");
                loadElement(op.getSingleOperand());
            }

        } else if (instType.equals(InstructionType.GETFIELD)) {
            addGetPutField(rhs);
        } else if (instType.equals(InstructionType.CALL)) {
            addCallInstruction(((CallInstruction) rhs), true);
        } else {
            // Unary operation
            UnaryOpInstruction op = (UnaryOpInstruction) rhs;
            addUnaryOperation(op);
            code.append("\t");
        }

        // Storing final value in variable
        if (dest instanceof ArrayOperand) code.append("iastore\n\t; End Array Assign\n\t");
        else storeElement(dest);

        code.append("\n\t; End Assign Instruction\n\n");

    }

    public void arrayAccess(Element elem) {
        code.append("\t; Start Array access\n\t");

        Operand variable = (Operand) elem;
        String name = variable.getName();
        code.append("aload ").append(getRegister(name)).append(" ; ").append(name); // load array
        code.append("\n\t");

        loadElement(((ArrayOperand) elem).getIndexOperands().get(0)); // load index
        code.append("iaload\n\t");
        code.append("; End Array access\n\t");
    }


    private void addUnaryOperation(UnaryOpInstruction op) {
        code.append("\n\t; Executing unary operation\n\t");

        // Load operand if needed to execute unary operation
        loadElement(op.getOperand());

        // Execute unary operation
        if (op.getOperation().getOpType().equals(OperationType.NOTB)) code.append("iconst_1\n\tixor\n");
        else if (op.getOperation().getOpType().equals(OperationType.SUB)) code.append("ineg\n");

        code.append("\t; End unary operation\n\n");
    }

    private void storeElement(Element dest) {

        Operand operand = (Operand) dest;
        if (operand.getType().getTypeOfElement().equals(ElementType.INT32) || operand.getType().getTypeOfElement().equals(ElementType.BOOLEAN))
            code.append("i"); // Number
        else code.append("a"); // Generic Object

        String name = ((Operand) dest).getName();
        int reg = Integer.parseInt(getRegister(name));

        if (reg < 4) code.append("store_").append(reg);
        else code.append("store ").append(reg);

        code.append(" ; ").append(name);
    }

    private void addBinaryOperation(BinaryOpInstruction opInstruction) {

        OperationType opType = opInstruction.getOperation().getOpType();

        code.append("\n\t; Executing binary operation\n\t");

        // Load operands
        loadElement(opInstruction.getLeftOperand());
        loadElement(opInstruction.getRightOperand());

        // execute operation
        BinaryOpInstAux(opType);

        code.append("\t; End binary operation\n\n ");
    }

    private void BinaryOpInstAux(OperationType opType) {

        switch (opType) {
            case ADD -> code.append("iadd\n");
            case SUB -> code.append("isub\n");
            case MUL -> code.append("imul\n");
            case DIV -> code.append("idiv\n");
            case LTH -> {
                String trueL = getNewLabel(), endL = getNewLabel();
                code.append("; Making lth operation\n\t");
                code.append("if_icmplt ").append(trueL).append("\n");
                code.append("\ticonst_0\n"); // false scope
                code.append("\tgoto ").append(endL).append("\n");
                code.append(trueL).append(":\n");
                code.append("\ticonst_1\n"); // true scope
                code.append(endL).append(":\n");
                code.append("; End lth operation\n");
            }
            case AND -> code.append("iand\n");
            default -> System.out.println("Binary op error");
        }
    }

    private void addInstruction(Instruction instruction) {

        String instType = instruction.getInstType().toString();

        // add label to instruction if needed
        var labels = currentMethod.getLabels(instruction);

        for (String label : labels)
            code.append(label).append(":\n");

        switch (instType) {
            case "ASSIGN" -> addInstructionAssign((AssignInstruction) instruction);
            case "CALL" -> addCallInstruction(((CallInstruction) instruction), false);
            case "GETFIELD", "PUTFIELD" -> addGetPutField(instruction);
            case "UNARYOPER" -> addUnaryOperation((UnaryOpInstruction) instruction);
            case "BINARYOPER" -> addBinaryOperation((BinaryOpInstruction) instruction);
            case "GOTO" -> code.append("\n\tgoto ").append(((GotoInstruction) instruction).getLabel()).append("\n");

            case "RETURN" -> {
                ReturnInstruction inst = (ReturnInstruction) instruction;
                code.append("\t");

                if (inst.getReturnType().toString().equals("VOID")) {
                    code.append("return");
                    return;
                }

                loadElement(inst.getOperand());

                switch (inst.getReturnType().toString()) {
                    case "INT32", "BOOLEAN" -> code.append("ireturn");
                    default -> code.append("areturn"); // ARRAYREF or OBJECTREF
                }

            }

            case "BRANCH" -> {
                if (instruction instanceof OpCondInstruction inst) addConditionalBranch(inst);
                else if (instruction instanceof SingleOpCondInstruction inst) addSingleConditionalBranch(inst);
                else System.out.println("Error in branch instruction");
            }


            default -> System.out.println("Error in instruction: " + instType);

        }

    }


    private void addGetPutField(Instruction instruction) {

        Operand op1, op2;
        String typeClass;

        if (instruction instanceof GetFieldInstruction inst) {
            op1 = (Operand) (inst).getFirstOperand();
            op2 = (Operand) (inst).getSecondOperand();
            typeClass = ((ClassType) op1.getType()).getName();

            code.append("\t");
            loadElement(op1);
            code.append("getfield").append(" ").append(typeClass).append("/");
            code.append(op2.getName()).append(" ").append(toJasminType(op2.getType().toString()));
            code.append("\n\t");
            return;
        }

        PutFieldInstruction inst = (PutFieldInstruction) instruction;

        op1 = (Operand) (inst).getFirstOperand(); // class name
        op2 = (Operand) (inst).getSecondOperand(); // field name where value will be stored
        Element op3 = (inst).getThirdOperand(); // value to be stored
        typeClass = ((ClassType) op1.getType()).getName();

        code.append("\t");
        loadElement(op1);
        loadElement(op3);
        code.append("putfield").append(" ").append(typeClass).append("/");
        code.append(op2.getName()).append(" ").append(toJasminType(op3.getType().toString()));
        code.append("\n\t");

    }

    private void addConditionalBranch(OpCondInstruction instruction) {
        OpInstruction opType = instruction.getCondition();
        String label = instruction.getLabel();

        code.append("\n\t; Executing conditional branch\n\t");

        if (opType.getOperands().size() != 2) {

            addUnaryOperation((UnaryOpInstruction) opType);
            code.append("ifne ").append(label).append("\n");

        } else {
            Element leftOperand = opType.getOperands().get(0);
            Element rightOperand = opType.getOperands().get(1);

            // Load operands needed to execute binary operation
            loadElement(leftOperand);
            loadElement(rightOperand);

            code.append("if_icmplt ").append(label).append("\n");
        }

        code.append("\t; End of conditional branch");
    }


    private void addSingleConditionalBranch(SingleOpCondInstruction instruction) {
        Operand op = (Operand) instruction.getCondition().getSingleOperand();
        String label = instruction.getLabel();

        code.append("\n\t; Executing conditional branch\n\t");
        loadElement(op);
        code.append("ifne ").append(label).append("\n");
        code.append("\t; End conditional branch\n\n");
    }


    private void addCallInstruction(CallInstruction inst, boolean isAssignment) {

        code.append("\t; Making a call instruction\n\t");
        switch ((inst.getInvocationType()).toString()) {
            case "NEW" -> callNew(inst);
            case "invokevirtual" -> callInvokeVirtual(inst, isAssignment);
            case "invokestatic" -> callInvokeStatic(inst, isAssignment);
            case "invokespecial" -> callInvokeSpecial(inst);
            case "ldc" ->
                    code.append("ldc ").append(((LiteralElement) inst.getFirstArg()).getLiteral()); // TODO: necessary?
            case "arraylength" -> {
                loadElement(inst.getFirstArg());
                code.append("\n\tarraylength ");
            }
            default -> System.out.println("Call instruction not supported");

        }
        code.append("\n\t; End of call instruction\n\t");
    }

    public void callNew(CallInstruction inst) {

        Element firstArg = inst.getFirstArg();

        if (firstArg.getType() instanceof ArrayType) {

            // load arguments
            for (Element arg : inst.getListOfOperands())
                loadElement(arg);

            // create new array (only int arrays are supported)
            code.append("\n\t newarray int");
            return;
        }

        String className = ((ClassType) firstArg.getType()).getName();

        // create new object
        code.append("\n\t; Creating new object\n\t");
        code.append("new ").append(className).append("\n\t");
        code.append("dup\n\t");
        code.append("; End of creating new object\n\t");
    }

    public void callInvokeVirtual(CallInstruction inst, boolean isAssignment) {

        loadElement(inst.getFirstArg());

        // make sure object cast is correct (needed for some reason)
        code.append("checkcast ").append(((ClassType) inst.getFirstArg().getType()).getName()).append("\n\t");

        // load arguments
        for (Element arg : inst.getListOfOperands())
            loadElement(arg);

        // invoke method
        code.append("invokevirtual ").append(((ClassType) inst.getFirstArg().getType()).getName());

        // add arguments names and types
        invokeArgs(inst, isAssignment);
    }


    private void invokeArgs(CallInstruction inst, boolean isAssignment) {

        code.append(".").append(((LiteralElement) inst.getSecondArg()).getLiteral().replace("\"", "")).append("(");

        // add arguments types
        for (Element arg : inst.getListOfOperands())
            code.append(toJasminType(arg.getType().toString()));

        code.append(")").append(toJasminType(inst.getReturnType().toString()));

        // if it is not an assign , just ignore the non-void return
        if (!isAssignment && !inst.getReturnType().getTypeOfElement().equals(ElementType.VOID)) code.append("\n\tpop");
    }

    public void callInvokeStatic(CallInstruction inst, boolean isAssignment) {

        String name = ((Operand) inst.getFirstArg()).getName();

        // load arguments
        for (Element arg : inst.getListOfOperands())
            loadElement(arg);

        // invoke method
        code.append("invokestatic ").append(name);

        // add arguments names and types
        invokeArgs(inst, isAssignment);
    }

    private void callInvokeSpecial(CallInstruction inst) {

        Element firstArg = inst.getFirstArg();


        boolean isThis = firstArg.getType().getTypeOfElement().equals(ElementType.THIS);

        if (isThis) code.append("\n\taload_0");
        else loadElement(firstArg);

        // load arguments
        for (Element arg : inst.getListOfOperands())
            loadElement(arg);

        code.append("\n\tinvokespecial ");

        // choose the classe name to use
        code.append(isThis ? classe.getClassName() : ((ClassType) firstArg.getType()).getName());

        // ================ making the init call ================

        code.append(".<init>(");

        // define argument types
        for (Element arg : inst.getListOfOperands())
            code.append(toJasminType(arg.getType().toString()));

        code.append(")V");

        if (!isThis) {
            String varName = ((Operand) firstArg).getName();
            int reg = Integer.parseInt(getRegister(varName));

            if (reg < 4) code.append("\n\tastore_").append(reg);
            else code.append("\n\tastore ").append(reg);

            code.append(" ; ").append(varName);
        }
    }


    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        this.classe = ollirResult.getOllirClass();

        addHeaders();
        addFields();
        addConstructor();
        addMethods();

        System.out.println("\n======================JASMIN CODE======================\n");
        System.out.println(code.toString());
        System.out.println("======================================================");

        return new JasminResult(code.toString());
    }

}

