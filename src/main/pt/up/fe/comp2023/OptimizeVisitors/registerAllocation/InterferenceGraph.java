package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.Element;

import java.util.*;

public class InterferenceGraph {

    Map<String, InterNode> nodes = new HashMap<>(); // TODO: aqui teve que ser com string porque o equals n estava a dar para Element

    public class InterNode {
        List<InterNode> edges = new ArrayList<>();
        String id;

        public InterNode(String id) {
            this.id = id;
        }

        public void addEdge(InterNode node) {
            edges.add(node);
        }

    }

    public InterferenceGraph() {

    }
     // https://gateoverflow.in/78311/how-to-draw-register-allocation-interference-graph
    public void addNode(Element element) {
        InterNode node = nodes.get(element);
        if (node == null) {
            node = new InterNode(element.toString());
            nodes.put(element.toString(), node);
        }
    }

    public void addEdge(Element element, Element element1) {

        if (! element.equals(element1)) {
            InterNode node = nodes.get(element.toString());
            InterNode node1 = nodes.get(element1.toString());

            node.addEdge(node1);
            node1.addEdge(node);
        }


    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("graph {\n");

        for (String element : nodes.keySet()) {
            sb.append(nodes.get(element).id);
            sb.append(" [label=\"");
            sb.append(element);
            sb.append("\"];\n");
        }

        for (String element : nodes.keySet()) {
            InterNode node = nodes.get(element);
            for (InterNode edge : node.edges) {
                if (edge.id != null && node.id != null) {
                    sb.append(node.id);
                    sb.append(" -- ");
                    sb.append(edge.id);
                    sb.append(";\n");
                }
            }
        }

        sb.append("}\n");
        return sb.toString();
    }
}
