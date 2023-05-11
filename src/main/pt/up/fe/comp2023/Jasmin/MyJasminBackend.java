package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.OptimizeVisitors.registerAllocation.RegisterAllocation;

import java.util.HashMap;

public class MyJasminBackend implements JasminBackend {

    boolean debug = false;

    ClassUnit classe;
    StringBuilder code = new StringBuilder();

    Method currentMethod;

    HashMap<String, Descriptor> currVarTable;

    int labelCounter = 0;
    int currentStack = 0;
    int maxStack = 0;

    int regNumAlloc = -1;

    // ============================================== MAIN METHOD ==============================================

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        this.classe = ollirResult.getOllirClass();

        var config = ollirResult.getConfig();
        this.debug = config.getOrDefault("debug", "false").equals("true");
        this.regNumAlloc = Integer.parseInt(config.getOrDefault("registerAllocation", "-1"));

        addHeaders();
        addFields();
        addConstructor();
        addMethods();

        if (debug) {
            System.out.println("\n======================JASMIN CODE======================\n");
            System.out.println(code.toString());
            System.out.println("======================================================");
        }
        return new JasminResult(code.toString());
    }

    // ============================================== AUXILIAR FUNCS ==============================================

    private void resetStack() {
        this.currentStack = 0;
        this.maxStack = 0;
    }

    private void updateStack(int stack) {
        this.currentStack += stack;
        this.maxStack = Math.max(this.currentStack, this.maxStack);
    }

    private void updateMethodLimits(int locals, int stack) {
        int lastIndex = code.lastIndexOf(".limit locals");
        code.replace(lastIndex, lastIndex + 16, ".limit locals " + locals);

        lastIndex = code.lastIndexOf(".limit stack");
        code.replace(lastIndex, lastIndex + 15, ".limit stack " + stack);
    }

    private String getRegister(String var) {
        if (currVarTable.containsKey(var)) return String.valueOf(currVarTable.get(var).getVirtualReg());
        return "-1";
    }

    private String getNewLabel() {
        return "label_" + labelCounter++;
    }

    private String toJasminType(Type type) {

        switch (type.getTypeOfElement().toString()) {
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
                if (type instanceof ClassType classType) return "L" + classType.getName() + ";";
                else if (type instanceof ArrayType arrayType) return "L" + (arrayType).getElementType() + ";";
                return "Ljava/lang/Object;";
            }
            case "STRING" -> {
                return "Ljava/lang/String;";
            }
            default -> {
                return "TypeError(" + type.getTypeOfElement() + ")";
            }
        }
    }

    // ============================================== BUILD METHODS ==============================================

    private void addHeaders() {
        code.append(".class public ").append(this.classe.getClassName()).append("\n");
        if (this.classe.getSuperClass() != null)
            code.append(".super ").append(this.classe.getSuperClass()).append("\n");
        else code.append(".super java/lang/Object\n");
    }

    public void addFields() {
        if (this.classe.getFields().size() == 0) return;
        if (debug) code.append("\n; Fields\n");
        this.classe.getFields().forEach(field -> {
            code.append(".field public ");
            code.append(field.getFieldName()).append(" ").append(toJasminType(field.getFieldType())).append("\n");
        });
    }

    public void addConstructor() {
        if (debug) code.append("\n; Constructor");
        code.append("\n.method public <init>()V");
        code.append("\n\taload_0");
        String aux = this.classe.getSuperClass() == null ? "java/lang/Object" : this.classe.getSuperClass();
        code.append("\n\tinvokespecial ").append(aux).append("/<init>()V");
        code.append("\n\treturn");
        code.append("\n.end method\n\n");
    }

    private void addMethods() {
        this.classe.getMethods().forEach(method -> {

            currVarTable = method.getVarTable();
            this.currentMethod = method;
            resetStack();

            boolean isMain = method.getMethodName().equals("main");

            if (isMain) code.append("\n.method public static main([Ljava/lang/String;)V\n");
            else if (method.getMethodName().equals(this.classe.getClassName())) return; // ignore constructor
            else code.append("\n.method public ").append(method.getMethodName()).append("(");

            if (!isMain) { // ignore constructor because its already defined
                method.getParams().forEach(param -> code.append(toJasminType(param.getType())));
                code.append(")").append(toJasminType(method.getReturnType())).append("\n");
            }

            // These values are changed later if reg optimization is enabled
            code.append("\t.limit stack 99").append("\n");
            code.append("\t.limit locals 99").append("\n\n");

            // add instructions
            method.getInstructions().forEach(instruction -> {
                addInstruction(instruction);
                code.append("\n");
            });

            // update method limits
            if (this.regNumAlloc < 0)
                updateMethodLimits(currVarTable.size() + (currVarTable.containsKey("this") ? 0 : 1), this.maxStack);
            else {
                if (regNumAlloc > 0) updateMethodLimits(this.regNumAlloc, this.maxStack);
                else {
                    int maxLocals = RegisterAllocation.nrC.values().stream().max(Integer::compareTo).get();
                    updateMethodLimits(maxLocals, this.maxStack);
                }
            }

            currVarTable = null;
            code.append(".end method\n\n");
        });
    }

    // ============================================== GENERIC INSTRUCTIONS ==============================================

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
            case "RETURN" -> addReturnInstruction((ReturnInstruction) instruction);
            case "BRANCH" -> addBranchInstruction(instruction);
            default -> System.out.println("Error in instruction: " + instType);
        }
    }

    private void addInstructionAssign(AssignInstruction instruction) {
        if (debug) code.append("\n\t; Assign Instruction\n");
        else code.append("\n");

        Element dest = instruction.getDest(); // instruction type <=> inst.getTypeOfAssign()
        Instruction rhs = instruction.getRhs();
        InstructionType instType = rhs.getInstType();

        if (dest instanceof ArrayOperand) {

            if (debug) code.append("\t; Start Array assign\n\t");
            else code.append("\t");

            // load array (cant use loadElement because IDK)
            arrayLoad((Operand) dest);
            code.deleteCharAt(code.length() - 1); // delete last character (new line)

            // value will be calculated below by the rhs instruction
        }

        if (instType.equals(InstructionType.BINARYOPER)) {
            // check if binop is iinc (x = x +/- N)
            if (tryAddInc(((Operand) dest), (BinaryOpInstruction) rhs)) {
                if (debug) code.append("\t; End Assign Instruction\n\n");
                else code.append("\n");
                return;
            } else
                addBinaryOperation((BinaryOpInstruction) rhs); // // if not, add binary operation loads and calculation
            code.append("\t");
        } else if (instType.equals(InstructionType.NOPER)) {
            SingleOpInstruction op = (SingleOpInstruction) rhs;

            if (op.getSingleOperand() instanceof ArrayOperand) arrayAccess(op.getSingleOperand());
            else {
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
        if (dest instanceof ArrayOperand) {
            code.append("iastore\n\t");
            if (debug) code.append("; End Array Assign\n\t");
            else code.append("\n\t");
            updateStack(-3); // pop array, index and value
        } else storeElement(dest);
        if (debug) code.append("\n\t; End Assign Instruction\n\n");
        else code.append("\n");
    }

    private boolean tryAddInc(Operand dest, BinaryOpInstruction rhs) {

        String op = rhs.getOperation().getOpType().toString();
        if (!op.equals("ADD") && !op.equals("SUB")) return false; // only add and sub are supported

        String opSign = op.equals("ADD") ? " + " : " - ";

        Element l = rhs.getLeftOperand();
        Element r = rhs.getRightOperand();

        if (l.isLiteral() && !r.isLiteral()) {
            Operand right = (Operand) r;
            LiteralElement left = (LiteralElement) l;
            int value = Integer.parseInt(left.getLiteral());

            // N +- X

            // iinc only supports values between -128 and 127
            if (value > 128) return false;
            else if (value == 128 && opSign.equals(" + ")) return false;

            // cases like N-X cant be done with iinc
            if (opSign.equals(" - ")) return false;

            // check if non literal is dest
            if (right.getName().equals(dest.getName())) {
                code.append("\tiinc ").append(getRegister(right.getName())).append(" ").append(value);
                if (debug)
                    code.append("\t; ").append(right.getName()).append(" = ").append(right.getName()).append(opSign).append(value);
                code.append("\n");
                return true;
            }
        } else if (r.isLiteral() && !l.isLiteral()) {
            Operand left = (Operand) l;
            LiteralElement right = (LiteralElement) r;
            int value = Integer.parseInt(right.getLiteral());

            // X +- N

            // iinc only supports values between -128 and 127
            if (value > 128) return false;
            else if (value == 128 && opSign.equals(" + ")) return false;

            // check if non literal is dest
            if (left.getName().equals(dest.getName())) {
                code.append("\tiinc ").append(getRegister(left.getName())).append(" ").append(opSign.equals(" + ") ? value : -value);
                if (debug)
                    code.append("\t; ").append(left.getName()).append(" = ").append(left.getName()).append(opSign).append(value);
                code.append("\n");
                return true;
            }
        }
        return false; // cant add iinc
    }

    private void addGetPutField(Instruction instruction) {

        Operand op1, op2;
        String typeClass;

        if (instruction instanceof GetFieldInstruction inst) {
            op1 = (Operand) (inst).getFirstOperand();
            op2 = (Operand) (inst).getSecondOperand();
            typeClass = ((ClassType) op1.getType()).getName();

            // getfield consumes 'this' and then pushes field value to stack
            code.append("\t");
            loadElement(op1); // load 'this' : stack + 1
            code.append("getfield").append(" ").append(typeClass).append("/");
            code.append(op2.getName()).append(" ").append(toJasminType(op2.getType()));
            code.append("\n\t"); // the field occupies the stack position of 'this'
            return;
        }

        PutFieldInstruction inst = (PutFieldInstruction) instruction;

        op1 = (Operand) (inst).getFirstOperand(); // class name
        op2 = (Operand) (inst).getSecondOperand(); // field name where value will be stored
        Element op3 = (inst).getThirdOperand(); // value to be stored
        typeClass = ((ClassType) op1.getType()).getName();

        code.append("\t");
        loadElement(op1); // load obj ref : stack + 1
        loadElement(op3); // load value to be stored : stack + 2
        code.append("putfield").append(" ").append(typeClass).append("/");
        updateStack(-2); // putfield consumes 2 values and pushes none
        code.append(op2.getName()).append(" ").append(toJasminType(op3.getType()));
        code.append("\n\t");
    }

    // ============================================== LOAD/STORE INSTRUCTIONS ==============================================

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
                if (value < 6 && value >= 0) code.append("iconst_");  // more efficient than bipush
                else if (value < 128 && value >= 0) code.append("bipush ");
                else if (value < 32768 && value >= 0) code.append("sipush ");
                else code.append("ldc ");
                code.append(value);
            } else {

                Operand variable = (Operand) element;
                String name = variable.getName();

                // Check if it is "true" or "false"
                if (isBoolean && name.equals("true")) code.append("iconst_1");
                else if (isBoolean && name.equals("false")) code.append("iconst_0");
                else {// it is a variable
                    int reg = Integer.parseInt(getRegister(name));
                    if (reg < 4) code.append("iload_").append(reg);
                    else code.append("iload ").append(reg);
                    if (debug) code.append(" ; ").append(name);
                }

            }
            updateStack(1); // push int
            code.append("\n\t");
            return;
        } else if (element.isLiteral()) { // string
            code.append("ldc ").append(((LiteralElement) element).getLiteral());
            code.append("\n\t");
            updateStack(1); // push string
            return;
        }

        // array or object reference
        Operand variable = (Operand) element;
        String name = variable.getName();

        int reg = Integer.parseInt(getRegister(name));
        if (reg < 4) code.append("aload_").append(reg);
        else code.append("aload ").append(reg);

        if (debug) code.append(" ; ").append(name);
        code.append("\n\t");
        updateStack(1); // push reference
    }

    private void storeElement(Element dest) {

        Operand operand = (Operand) dest;
        boolean isInt = operand.getType().getTypeOfElement().equals(ElementType.INT32);
        boolean isBool = operand.getType().getTypeOfElement().equals(ElementType.BOOLEAN);
        if (isInt || isBool) code.append("i"); // Number
        else code.append("a"); // Generic Object

        String name = ((Operand) dest).getName();
        int reg = Integer.parseInt(getRegister(name));

        if (reg < 4) code.append("store_").append(reg);
        else code.append("store ").append(reg);
        updateStack(-1); // pop value from stack and store it

        if (debug) code.append(" ; ").append(name);
    }

    // ============================================== ARRAY INSTRUCTIONS ==============================================

    private void arrayLoad(Operand elem) {
        String name = elem.getName();
        int reg = Integer.parseInt(getRegister(name));
        // load array
        if (reg < 4) code.append("aload_").append(reg);
        else code.append("aload ").append(reg);
        if (debug) code.append(" ; ").append(name);
        code.append("\n\t");
        updateStack(1); // push array
        loadElement(((ArrayOperand) elem).getIndexOperands().get(0)); // load index
    }

    public void arrayAccess(Element elem) {
        if (debug) code.append("\t; Start Array access\n\t");
        else code.append("\t");
        arrayLoad((Operand) elem);
        code.append("iaload\n\t");
        updateStack(-1); // pop array, index and push value in stack
        if (debug) code.append("; End Array access\n\t");
        else code.append("\n\t");
    }

    // ============================================== CALL/RETURN INSTRUCTIONS ==============================================

    private void addCallInstruction(CallInstruction inst, boolean isAssignment) {

        if (debug) code.append("\t; Making a call instruction\n\t");
        else code.append("\n\t");
        switch ((inst.getInvocationType()).toString()) {
            case "NEW" -> callNew(inst);
            case "invokevirtual" -> callInvokeVirtual(inst, isAssignment);
            case "invokestatic" -> callInvokeStatic(inst, isAssignment);
            case "invokespecial" -> callInvokeSpecial(inst);
            case "ldc" -> {
                code.append("ldc ").append(((LiteralElement) inst.getFirstArg()).getLiteral());
                updateStack(1); // ldc pushes a value to the stack
            }
            case "arraylength" -> {
                // arraylength pops an array ref and pushes its length
                loadElement(inst.getFirstArg());
                code.append("\n\tarraylength ");
            }
            default -> System.out.println("Call instruction not supported");
        }
        if (debug) code.append("\n\t; End of call instruction\n\t");
        else code.append("\n\t");
    }

    public void callNew(CallInstruction inst) {

        Element firstArg = inst.getFirstArg();

        if (firstArg.getType() instanceof ArrayType) {

            // load arguments
            for (Element arg : inst.getListOfOperands())
                loadElement(arg);

            // create new array (only int arrays are supported)
            code.append("\n\t newarray int\n\t");
            updateStack(1); // newarray pushes new array ref to stack
            return;
        }

        String className = ((ClassType) firstArg.getType()).getName();

        // create new object
        if (debug) code.append("\n\t; Creating new object\n\t");
        else code.append("\n\t");
        code.append("new ").append(className).append("\n\t");
        code.append("dup\n\t");
        updateStack(2);
        if (debug) code.append("; End of creating new object\n\t");
        else code.append("\n\t");
    }

    public void callInvokeVirtual(CallInstruction inst, boolean isAssignment) {

        loadElement(inst.getFirstArg()); // load object reference

        for (Element arg : inst.getListOfOperands())
            loadElement(arg); // load arguments

        // invoke method
        String methodName = ((ClassType) inst.getFirstArg().getType()).getName();
        code.append("invokevirtual ").append(methodName);

        invokeArgs(inst, isAssignment); // add arguments names and types
    }

    public void callInvokeStatic(CallInstruction inst, boolean isAssignment) {

        String name = ((Operand) inst.getFirstArg()).getName();

        for (Element arg : inst.getListOfOperands())
            loadElement(arg); // load arguments

        code.append("invokestatic ").append(name); // invoke method

        invokeArgs(inst, isAssignment); // add arguments names and types
    }

    private void invokeArgs(CallInstruction inst, boolean isAssignment) {

        String aux = ((LiteralElement) inst.getSecondArg()).getLiteral().replace("\"", "");
        code.append(".").append(aux).append("(");

        // add arguments types
        for (Element arg : inst.getListOfOperands())
            code.append(toJasminType(arg.getType()));

        code.append(")").append(toJasminType(inst.getReturnType()));

        // update stack
        // updateStack(-1); // pop obj reference --> not needed because it is a static method
        updateStack(-inst.getListOfOperands().size()); // pop arguments

        if (!inst.getReturnType().getTypeOfElement().equals(ElementType.VOID)) {
            updateStack(1); // push space for return value to stack

            // if it is not an assign , just ignore the non-void return
            if (!isAssignment) {
                code.append("\n\tpop");
                updateStack(-1); // pop return value
            }
        }
    }

    private void callInvokeSpecial(CallInstruction inst) {

        Element firstArg = inst.getFirstArg();
        boolean isThis = firstArg.getType().getTypeOfElement().equals(ElementType.THIS);

        if (isThis) {
            code.append("\n\taload_0");
            updateStack(1); // push this to stack
        } else loadElement(firstArg);

        for (Element arg : inst.getListOfOperands())
            loadElement(arg); // load arguments

        code.append("\n\tinvokespecial ");

        // choose the classe name to use
        code.append(isThis ? classe.getClassName() : ((ClassType) firstArg.getType()).getName());

        // ================ making the init call ================

        code.append(".<init>(");

        // define argument types
        for (Element arg : inst.getListOfOperands())
            code.append(toJasminType(arg.getType()));

        code.append(")V");

        // update stack
        updateStack(-1); // pop obj reference
        updateStack(-inst.getListOfOperands().size()); // pop arguments

        if (!isThis) {
            // no need to update stack in this case ?
            String varName = ((Operand) firstArg).getName();
            int reg = Integer.parseInt(getRegister(varName));
            if (reg < 4) code.append("\n\tastore_").append(reg);
            else code.append("\n\tastore ").append(reg);
            if (debug) code.append(" ; ").append(varName);
        }
    }

    private void addReturnInstruction(ReturnInstruction inst) {
        code.append("\t");

        if (inst.getReturnType().toString().equals("VOID")) {
            code.append("return");
            return;
        }

        loadElement(inst.getOperand()); // load return value

        switch (inst.getReturnType().toString()) {
            case "INT32", "BOOLEAN" -> code.append("ireturn");
            default -> code.append("areturn"); // ARRAYREF or OBJECTREF
        }
    }

    // ============================================== BINARY/UNARY INSTRUCTIONS ==============================================
    private void addBinaryOperation(BinaryOpInstruction opInstruction) {

        OperationType opType = opInstruction.getOperation().getOpType();

        if (debug) code.append("\n\t; Executing binary operation\n\t");
        else code.append("\n\t");

        Element left = opInstruction.getLeftOperand();
        Element right = opInstruction.getRightOperand();

        if (opType.name().equals("LTH")) addLTHOp(left, right);
        else {
            loadElement(left);
            loadElement(right);

            // execute operation
            // binary ops pop 2 values and push 1 to stack
            switch (opType) {
                case ADD -> {
                    code.append("iadd\n");
                    updateStack(-1);
                }
                case SUB -> {
                    code.append("isub\n");
                    updateStack(-1);
                }
                case MUL -> {
                    code.append("imul\n");
                    updateStack(-1);
                }
                case DIV -> {
                    code.append("idiv\n");
                    updateStack(-1);
                }
                case ANDB -> {
                    code.append("iand\n");
                    updateStack(-1);
                }
                default -> System.out.println("Binary op error + " + opType + " not implemented");
            }
        }

        if (debug) code.append("\t; End binary operation\n\n ");
        else code.append("\n");
    }

    private void addLTHOp(Element left, Element right) {

        // case 0 < A
        if (left.isLiteral()) {
            int value = Integer.parseInt(((LiteralElement) left).getLiteral());
            if (value == 0) {
                loadElement(right);
                addBooleanTrueResult("ifgt");
                return;
            }
        }

        // case A < 0
        if (right.isLiteral()) {
            int value = Integer.parseInt(((LiteralElement) right).getLiteral());
            if (value == 0) {
                loadElement(left);
                addBooleanTrueResult("iflt");
                return;
            }
        }

        // case A < B
        loadElement(left);
        loadElement(right);
        addBooleanTrueResult("if_icmplt");
    }

    private void addUnaryOperation(UnaryOpInstruction op) {
        if (debug) code.append("\n\t; Executing unary operation\n\t");
        else code.append("\n\t");

        // Load operand needed to execute unary operation
        loadElement(op.getOperand());

        // Execute unary operation
        if (op.getOperation().getOpType().equals(OperationType.NOTB)) addBooleanTrueResult("ifeq");
        else if (op.getOperation().getOpType().equals(OperationType.SUB)) code.append("ineg\n");
        else System.out.println("Unary op error");

        if (debug) code.append("\t; End unary operation\n\n");
        else code.append("\n");
    }

    // ============================================== CONDITIONAL INSTRUCTIONS ==============================================

    private void addBooleanTrueResult(String jumpCondition) {
        if (debug) code.append("; Boolean Calculation\n\t");
        else code.append("\n\t");

        String trueL = getNewLabel(), endL = getNewLabel();

        code.append(jumpCondition).append(" ").append(trueL).append("\n");
        updateStack(jumpCondition.equals("if_icmplt") ? -2 : -1); // pop values used for comparison

        code.append("\ticonst_0\n"); // false value
        code.append("\tgoto ").append(endL).append("\n");
        code.append(trueL).append(":\n");
        code.append("\ticonst_1\n"); // true value
        updateStack(1); // push one of these : true or false
        code.append(endL).append(":\n");

        if (debug) code.append("; End of Boolean Calculation\n");
        else code.append("\n");
    }

    private void addBranchInstruction(Instruction instruction) {
        if (instruction instanceof OpCondInstruction inst) addConditionalBranch(inst);
        else if (instruction instanceof SingleOpCondInstruction inst) addSingleConditionalBranch(inst);
        else System.out.println("Error in branch instruction");
    }

    private void addConditionalBranch(OpCondInstruction instruction) {
        OpInstruction opType = instruction.getCondition();
        String label = instruction.getLabel();

        if (debug) code.append("\n\t; Executing Conditional branch\n\t");
        else code.append("\n\t");

        if (opType instanceof BinaryOpInstruction condInstruction) addBinaryOperation(condInstruction);
        else if (opType instanceof UnaryOpInstruction condInstruction) addUnaryOperation(condInstruction);

        code.append("\n\t").append("ifne ").append(label).append("\n");

        if (debug) code.append("\t; End of Conditional branch");
        else code.append("\n");
    }


    private void addSingleConditionalBranch(SingleOpCondInstruction instruction) {
        Element elem = instruction.getCondition().getSingleOperand();
        String label = instruction.getLabel();

        if (debug) code.append("\n\t; Executing single op conditional branch\n\t");
        else code.append("\n\t");

        loadElement(elem); // load variable

        code.append("ifne ").append(label).append("\n");
        updateStack(-1); // pop value used for comparison
        if (debug) code.append("\t; End single op conditional branch\n\n");
        else code.append("\n");
    }

}

