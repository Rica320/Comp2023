package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Method;

import java.util.*;

public class RegisterAllocation {

    // https://www.cs.purdue.edu/homes/hosking/502/notes/08-reg.pdf

    int nr_registers;
    private ClassUnit classUnit;

    public RegisterAllocation(ClassUnit classUnit, int nr_registers) {
        this.classUnit = classUnit;
        this.nr_registers = nr_registers;
    }

    public void run() {
        classUnit.getMethods().forEach(method -> {
            System.out.println("method: " + method.getMethodName());
            List<GraphNode> nodes = liveliness(method);

            InterferenceGraph interferenceGraph = interferenceGraph(nodes);

            coloring(interferenceGraph);
            System.out.println(interferenceGraph);
        });
    }

    public InterferenceGraph interferenceGraph(List<GraphNode> nodes) {
        // ...https://www.hcltech.com/sites/default/files/documents/resources/whitepaper/files/register_allocation_via_graph_coloring_meena_jain_-_v2.0.pdf

        InterferenceGraph interferenceGraph = new InterferenceGraph();

        for (GraphNode node : nodes) {
            Set<Element> def = node.getDef();
            Set<Element> out = node.getOut();


            for (Element defElement : def) {
                interferenceGraph.addNode(defElement);
                for (Element useElement : out) {
                    interferenceGraph.addNode(useElement);
                    interferenceGraph.addEdge(defElement, useElement);
                }
            }
        }

        return interferenceGraph;
    }

    public InterferenceGraph.InterNode getLowestDegreeNode(InterferenceGraph interferenceGraph) {
        InterferenceGraph.InterNode lowestDegreeNode = null;
        for (InterferenceGraph.InterNode node : interferenceGraph.nodes.values()) {
            if (node.getDegree() < nr_registers && node.getDegree() != -1) {
                nr_registers = node.getDegree();
                lowestDegreeNode = node;
            }
        }
        return lowestDegreeNode;
    }

    public List<InterferenceGraph.InterNode> spillingNodes(InterferenceGraph interferenceGraph) {
        List<InterferenceGraph.InterNode> nodes = new ArrayList<>();
        for (InterferenceGraph.InterNode node : interferenceGraph.nodes.values()) {
            if (node.getDegree() >= nr_registers) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    public void colorNodes(Stack<InterferenceGraph.InterNode> stack) {
        while (!stack.isEmpty()) {
            InterferenceGraph.InterNode node = stack.pop();
            int color = node.getDegree();
            for (InterferenceGraph.InterNode neighbor : node.edges) {
                if (neighbor.getDegree() == color) {
                    color++;
                }
                neighbor.incrementDegree();
            }
            node.setColor(color);
        }
    }

    public int coloring(InterferenceGraph interferenceGraph) {

        Stack<InterferenceGraph.InterNode> stack = new Stack<>();


        do {
            InterferenceGraph.InterNode lowestDegreeNode = getLowestDegreeNode(interferenceGraph);
            if (lowestDegreeNode == null) {
                break;
            }

            for (InterferenceGraph.InterNode node : lowestDegreeNode.edges) {
                node.decrementDegree();
            }
            lowestDegreeNode.takeFromGraph();
            stack.push(lowestDegreeNode);

        } while (true);

        List<InterferenceGraph.InterNode> toSplit = spillingNodes(interferenceGraph);

        for (InterferenceGraph.InterNode node : toSplit) {
            node.takeFromGraph();
            stack.push(node);
        }
        colorNodes(stack);

        // TODO : spilling


        return 0;
    }

    public List<GraphNode> liveliness(Method method) {

        method.buildCFG();

        List<GraphNode> nodes = GraphNode.getFromNodes(method.getInstructions());

        boolean changed;
        do {
            changed = true;
            for (GraphNode node : nodes) {

                Set<Element> inL = new HashSet<>(node.getIn()); // in'
                Set<Element> outL = new HashSet<>(node.getOut()); // out'


                Set<Element> use = node.getUse();
                Set<Element> def = node.getDef();

                // in[n] = use[n] U (out[n] - def[n])
                Set<Element> diff = new HashSet<>(node.getOut());
                diff.removeAll(def);

                use.addAll(diff);

                node.setIn(use);

                Set<Element> outAux = new HashSet<>();
                for (GraphNode successor : node.getSuccessors()) {
                    outAux.addAll(successor.getIn());
                }
                node.setOut(outAux);

                if (!inL.equals(node.getIn()) || !outL.equals(node.getOut())) {
                    changed = false;
                }
            }
        } while (!changed);

        for (GraphNode node : nodes) {
            System.out.println(node.getInstruction());
            System.out.println(node.getInstruction().getClass());
            System.out.println("in: " + node.getIn());
            System.out.println("out: " + node.getOut());
            System.out.println("use: " + node.getUse());
            System.out.println("def: " + node.getDef());
            System.out.println();
        }


        return nodes;

    }
}
