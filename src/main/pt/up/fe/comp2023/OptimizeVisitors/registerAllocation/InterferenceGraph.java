package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Operand;

import java.util.*;

public class InterferenceGraph {

    Map<String, InterNode> nodes = new HashMap<>(); // TODO: aqui teve que ser com string porque o equals n estava a dar para Element

    public class InterNode {
        public List<InterNode> edges = new ArrayList<>();
        int degree = -1;
        String id;
        Element element;

        public InterNode(String id, Element element) {
            this.id = id;
            this.element = element;
        }

        public void addEdge(InterNode node) {
            if (degree == -1)
                degree = 1;
            else
                degree++;
            edges.add(node);
        }

        public int getDegree() {
            return degree;
        }

        public void takeFromGraph() {
            degree = -1;
        }

        public void decrementDegree() {
            if (degree > 0) degree--;
        }

        public void setColor(int degree, HashMap<String, Descriptor> descriptors) {
            id = "color" + degree;

            Operand operand = (Operand) element;
            Descriptor descriptor = descriptors.get(operand.getName());
            descriptor.setVirtualReg(degree);
            //operand.setName(id);
        }

        public void incrementDegree() {
            degree++;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public InterferenceGraph() {

    }
     // https://gateoverflow.in/78311/how-to-draw-register-allocation-interference-graph
    public void addNode(Element element) {
        InterNode node = nodes.get(element.toString());
        if (node == null) {
            node = new InterNode(element.toString(), element);
            nodes.put(element.toString(), node);
        }
    }

    public void addEdge(Element element, Element element1) {

        if (! element.toString().equals(element1.toString())) {
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
