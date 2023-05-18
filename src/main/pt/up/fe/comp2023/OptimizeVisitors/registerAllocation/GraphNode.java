package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphNode {
    private final List<GraphNode> predecessors = new ArrayList<>();
    private final List<GraphNode> successors = new ArrayList<>();
    private final Set<Element> in = new HashSet<>();
    private final Set<Element> out = new HashSet<>();
    private final Set<Element> use = new HashSet<>();
    private final Set<Element> def = new HashSet<>();
    private Instruction instruction;


    void addUseIfNotLiteral(List<Element> list) {
        for (Element element : list) {
            if (element instanceof Operand operand) {
                if (!operand.isLiteral() && !operand.isParameter())
                    addUse(element);
            }
        }
    }

    void fillUseAndDef(Instruction instruction) {
        switch (instruction.getInstType()) {
            case CALL -> {
                CallInstruction callInstruction = (CallInstruction) instruction;
                this.addUseIfNotLiteral(callInstruction.getListOfOperands());
            }
            case BRANCH -> {
                System.out.println(instruction.getClass());
                if (instruction instanceof OpCondInstruction opCondInstruction) {
                    this.addUseIfNotLiteral(opCondInstruction.getOperands());
                }
            }
            case RETURN -> {
                ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                if (returnInstruction.getOperand() != null
                        && !returnInstruction.getOperand().isLiteral())
                    addUse(returnInstruction.getOperand());
            }
            case GOTO -> {
                // GotoInstruction gotoInstruction = (GotoInstruction) instruction;

            }
            case ASSIGN -> {
                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                fillUseAndDef(assignInstruction.getRhs());
                addDef(assignInstruction.getDest());
            }
            case PUTFIELD -> {
                PutFieldInstruction putFieldInstruction = (PutFieldInstruction) instruction;
                this.addUseIfNotLiteral(putFieldInstruction.getOperands());
            }
            case GETFIELD -> {
                GetFieldInstruction getFieldInstruction = (GetFieldInstruction) instruction;
                this.addUseIfNotLiteral(getFieldInstruction.getOperands());
            }
            case UNARYOPER -> {
                UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) instruction;
                this.addUseIfNotLiteral(unaryOpInstruction.getOperands());
            }
            case BINARYOPER -> {
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instruction;
                this.addUseIfNotLiteral(binaryOpInstruction.getOperands());
                System.out.println(binaryOpInstruction.getOperation());
            }
            case NOPER -> {
                SingleOpInstruction singleOpInstruction = (SingleOpInstruction) instruction;
                if (singleOpInstruction.getSingleOperand() != null
                        && !singleOpInstruction.getSingleOperand().isLiteral())
                    addUse(singleOpInstruction.getSingleOperand());
            }
        }
    }

    public GraphNode(Instruction instruction) {
        this.instruction = instruction;

        fillUseAndDef(instruction);

    }

    static List<GraphNode> getFromNodes(List<Instruction> instructions) {
        List<GraphNode> nodes = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (Instruction instruction : instructions) {
            nodes.add(new GraphNode(instruction));
            ids.add(instruction.getId());
        }
        for (GraphNode node : nodes) {
            List<Node> succ = node.getInstruction().getSuccessors();

            for (Node n: succ) {
                int index = ids.indexOf(n.getId());
                if (index == -1) continue;
                node.addSuccessor(nodes.get(index));
            }

            List<Node> pred = node.getInstruction().getPredecessors();

            for (Node n : pred) {
                int index = ids.indexOf(n);
                if (index == -1) continue;
                node.addPredecessor(nodes.get(index));
            }
        }
        return nodes;
    }

    public void addDef(Element var) {
        def.add(var);
    }

    public void addUse(Element var) {
        use.add(var);
    }

    public void addPredecessor(GraphNode predecessor) {
        predecessors.add(predecessor);
    }

    public void addSuccessor(GraphNode successor) {
        successors.add(successor);
    }

    public List<GraphNode> getPredecessors() {
        return predecessors;
    }

    public List<GraphNode> getSuccessors() {
        return successors;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public Set<Element> getIn() {
        return in;
    }

    public void setIn(Set<Element> in) {
        this.in.clear();
        this.in.addAll(in);
    }

    public Set<Element> getOut() {
        return out;
    }

    public void setOut(Set<Element> out) {
        this.out.clear();
        this.out.addAll(out);
    }

    public Set<Element> getUse() {
        return use;
    }

    public Set<Element> getDef() {
        return def;
    }

    @Override
    public String toString() {
        return instruction.toString();
    }
}
