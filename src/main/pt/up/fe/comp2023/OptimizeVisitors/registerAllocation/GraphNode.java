package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphNode {
    private final List<GraphNode> predecessors = new ArrayList<>();
    private final List<GraphNode> successors = new ArrayList<>();

    private Instruction instruction;

    private final Set<Element> in = new HashSet<>();
    private final Set<Element> out = new HashSet<>();
    private final Set<Element> use = new HashSet<>();
    private final Set<Element> def = new HashSet<>();

    public GraphNode(Instruction instruction) {
        this.instruction = instruction;


        switch (instruction.getInstType()) {
            case CALL -> {
            }
            case BRANCH -> {

            }
            case RETURN -> {
                ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                addUse(returnInstruction.getOperand());
            }
            case GOTO -> {
            }
            case ASSIGN -> {
                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                if (assignInstruction.getRhs().getInstType() == InstructionType.NOPER) {
                    addUse(((SingleOpInstruction) assignInstruction.getRhs()).getSingleOperand());
                }
                addDef(assignInstruction.getDest());
            }
            case PUTFIELD -> {

            }
            case GETFIELD -> {

            }
            case UNARYOPER -> {

            }
            case BINARYOPER -> {

            }
            case NOPER -> {

            }

        }
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

    public Set<Element> getOut() {
        return out;
    }

    public Set<Element> getUse() {
        return use;
    }

    public Set<Element> getDef() {
        return def;
    }

    static List<GraphNode> getFromNodes(List<Instruction> instructions) {
        List<GraphNode> nodes = new ArrayList<>();
        ///List<Integer> ids = new ArrayList<>();
        for (Instruction instruction : instructions) {
            nodes.add(new GraphNode(instruction));
           // ids.add(instruction.getId());
        }
        for (GraphNode node : nodes) { // TODO: n é o melhor em termos de alg
            for (Node successor : instructions) {
                if (successor.getSuccessors().contains(node.getInstruction())) {
                    node.addPredecessor(nodes.get(successor.getId() - 1));
                }
            }
            for (Node predecessor : instructions) {
                if (predecessor.getPredecessors().contains(node.getInstruction())) {
                    node.addSuccessor(nodes.get(predecessor.getId() - 1));
                }
            }
        }
        return nodes;
    }

    @Override
    public String toString() {
        return instruction.toString();
    }

    public void setOut(Set<Element> out) {
        this.out.clear();
        this.out.addAll(out);
    }

    public void setIn(Set<Element> in) {
        this.in.clear();
        this.in.addAll(in);
    }
}
