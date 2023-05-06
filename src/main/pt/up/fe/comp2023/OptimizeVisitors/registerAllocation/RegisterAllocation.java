package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Element;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegisterAllocation {

    // https://www.cs.purdue.edu/homes/hosking/502/notes/08-reg.pdf

    private ClassUnit classUnit;

    public RegisterAllocation(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public void liveliness() {


        classUnit.getMethods().forEach(method -> {
            // method.buildCFG(); .... cfg ??? perguntar ao professor o pk de este metodo existir
            method.buildCFG();

            List<GraphNode> nodes = GraphNode.getFromNodes(method.getInstructions());

            int i = 1;
            boolean changed;
            do {
                changed = true;
                for (GraphNode node : nodes) {

                    Set<Element> inL = node.getIn(); // in'
                    Set<Element> outL = node.getOut(); // out'


                    Set<Element> use = node.getUse();
                    Set<Element> def = node.getDef();

                    // in[n] = use[n] U (out[n] - def[n])
                    Set<Element> diff = new HashSet<>(node.getOut());
                    diff.removeAll(def);

                    use.addAll(diff);

                    node.setIn(use);

                    Set<Element> outAux = new HashSet<>();
                    for (GraphNode successor : node.getSuccessors()) {
                        System.out.println("successor: " + successor.getInstruction());
                        System.out.println(successor.getIn());
                        System.out.println(successor.getOut());
                        System.out.println(successor.getUse());
                        System.out.println(successor.getDef());

                        outAux.addAll(successor.getIn());
                    }
                    System.out.println("outAux: " + outAux);
                    node.setOut(outAux); // ver se é necessário

                    System.out.println("nr.Interations: " + i );
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

        });


    }
}
