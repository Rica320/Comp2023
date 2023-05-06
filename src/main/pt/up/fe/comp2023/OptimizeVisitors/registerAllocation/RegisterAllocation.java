package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Node;

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

            boolean changed;
            do {
                changed = false;
                for (GraphNode node : nodes) {

                    Set<Integer> inL = node.getIn(); // in'
                    Set<Integer> outL = node.getOut(); // out'


                    Set<Integer> use = node.getUse();
                    Set<Integer> def = node.getDef();

                    // in[n] = use[n] U (out[n] - def[n])
                    Set<Integer> diff = new HashSet<>(node.getOut());
                    diff.removeAll(def);

                    use.addAll(diff);

                    node.setIn(use);

                    Set<Integer> outAux = new HashSet<>();
                    for (GraphNode successor : node.getSuccessors()) {
                        outAux.addAll(successor.getIn());
                    }

                    node.setOut(outAux); // ver se é necessário

                    if (!inL.equals(node.getIn()) || !outL.equals(node.getOut())) {
                        changed = true;
                    }

                }
            } while (!changed);

        });


    }
}
