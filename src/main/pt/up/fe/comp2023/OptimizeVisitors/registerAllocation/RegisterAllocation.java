package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.*;

import java.util.*;

public class RegisterAllocation {

    // https://www.cs.purdue.edu/homes/hosking/502/notes/08-reg.pdf

    int nr_registers;
    private ClassUnit classUnit;
    HashMap<String, Descriptor> descriptors;
    public static Map<String, Integer> nrC = new HashMap<>();
    Method currentMethod;

    public RegisterAllocation(ClassUnit classUnit, int nr_registers) {
        this.classUnit = classUnit;
        this.nr_registers = nr_registers;
    }

    public void run() {
        for (Method method : classUnit.getMethods()) {
            System.out.println("method: " + method.getMethodName());
            this.descriptors = method.getVarTable();
            this.currentMethod = method;
            List<GraphNode> nodes = liveliness(method);

            InterferenceGraph interferenceGraph = interferenceGraph(nodes);

            int nr_colors = coloring(interferenceGraph);
            nrC.put(method.getMethodName(), nr_colors);
            System.out.println(interferenceGraph);
        }
    }

    public InterferenceGraph interferenceGraph(List<GraphNode> nodes) {
        // ...https://www.hcltech.com/sites/default/files/documents/resources/whitepaper/files/register_allocation_via_graph_coloring_meena_jain_-_v2.0.pdf

        InterferenceGraph interferenceGraph = new InterferenceGraph();

        for (GraphNode node : nodes) {
            Set<Element> def = node.getDef();
            Set<Element> out = node.getOut();


            for (Element defElement : def) {
                if (defElement == null) continue;
                interferenceGraph.addNode(defElement);
                for (Element useElement : out) {
                    if (useElement == null) continue;
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

    public int colorNodes(Stack<InterferenceGraph.InterNode> stack) {
        Set<Integer> colors = new HashSet<>();
        while (!stack.isEmpty()) {
            InterferenceGraph.InterNode node = stack.pop();
            int color = currentMethod.getParams().size() + (currentMethod.isStaticMethod() ? 0 : 1);
            for (InterferenceGraph.InterNode neighbor : node.edges) {
                if (neighbor.id.equals("color" + color)) {
                    color++;
                }
            }
            colors.add(color);
            node.setColor(color, this.descriptors);
        }
        System.out.println("colors: " + colors);
        return colors.size();
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

        int colorsUsed = colorNodes(stack);

        //TODO : spilling ... should we do it ?


        return colorsUsed;
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
