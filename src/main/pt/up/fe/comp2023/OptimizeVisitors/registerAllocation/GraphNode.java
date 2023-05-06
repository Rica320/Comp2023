package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphNode {
    private final List<GraphNode> predecessors = new ArrayList<>();
    private final List<GraphNode> successors = new ArrayList<>();

    private Instruction instruction;

    private final Set<Integer> in = new HashSet<>();
    private final Set<Integer> out = new HashSet<>();
    private final Set<Integer> use = new HashSet<>();
    private final Set<Integer> def = new HashSet<>();

    public GraphNode(Instruction instruction) {
        this.instruction = instruction;
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

    public Set<Integer> getIn() {
        return in;
    }

    public Set<Integer> getOut() {
        return out;
    }

    public Set<Integer> getUse() {
        return use;
    }

    public Set<Integer> getDef() {
        return def;
    }

    static List<GraphNode> getFromNodes(List<Instruction> instructions) {
        List<GraphNode> nodes = new ArrayList<>();
        for (Instruction instruction : instructions) {
            nodes.add(new GraphNode(instruction));
        }
        return nodes;
    }

    @Override
    public String toString() {
        return instruction.toString();
    }

    public void setOut(Set<Integer> out) {
        this.out.clear();
        this.out.addAll(out);
    }

    public void setIn(Set<Integer> in) {
        this.in.clear();
        this.in.addAll(in);
    }
}
