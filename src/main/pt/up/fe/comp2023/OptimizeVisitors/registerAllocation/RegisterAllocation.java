package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Method;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegisterAllocation {

    // https://www.cs.purdue.edu/homes/hosking/502/notes/08-reg.pdf

    private ClassUnit classUnit;

    public RegisterAllocation(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public void run() {
        classUnit.getMethods().forEach(method -> {
            System.out.println("method: " + method.getMethodName());
            List<GraphNode> nodes = liveliness(method);

            InterferenceGraph interferenceGraph = interferenceGraph(nodes);
            System.out.println(interferenceGraph);
        });
    }

    public InterferenceGraph interferenceGraph(List<GraphNode> nodes) {

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

        System.out.println(interferenceGraph);

        return interferenceGraph;


    }

    public List<GraphNode> liveliness(Method method) {

        method.buildCFG();

        List<GraphNode> nodes = GraphNode.getFromNodes(method.getInstructions());

        int i = 1;
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
                node.setOut(outAux); // ver se é necessário

                if (!inL.equals(node.getIn()) || !outL.equals(node.getOut())) {
                    i++;
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
