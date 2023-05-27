package pt.up.fe.comp2023.OptimizeVisitors.registerAllocation;

import org.specs.comp.ollir.*;

import java.util.*;

public class RegisterAllocation {

    // https://www.cs.purdue.edu/homes/hosking/502/notes/08-reg.pdf

    public static Map<String, Integer> nrC = new HashMap<>();
    int nr_registers;
    HashMap<String, Descriptor> descriptors;
    Method currentMethod;
    private ClassUnit classUnit;

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
        if (nr_registers == 0) nr_registers = -1;
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
        //System.out.println("VAR TABLE");
        //System.out.println(currentMethod.getVarTable());
       // int lastReg = 0;
       // for (Descriptor descriptor : currentMethod.getVarTable().values()) {
       //     if (!(descriptor.getScope() == VarScope.LOCAL)) {
       //         continue;
       //     }
       // }
        //System.out.println("lastReg: " + lastReg);
        // Instruction inst;
        // currentMethod.getVarTable().clear();
        // int varID = 0;
        // Iterator var2;
        // for(var2 = currentMethod.getInstructions().iterator();
        //     var2.hasNext(); varID = currentMethod.addToVartable(inst, varID)) {
        //     inst = (Instruction)var2.next();
        // }
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

        int wantedColors = nr_registers;

        do {
            InterferenceGraph.InterNode lowestDegreeNode = getLowestDegreeNode(interferenceGraph);
            System.out.println("lowestDegreeNode: " + lowestDegreeNode);
            if (lowestDegreeNode == null) {
                break;
            }

            for (InterferenceGraph.InterNode node : lowestDegreeNode.edges) {
                node.decrementDegree();
            }
            lowestDegreeNode.takeFromGraph();
            System.out.println("Pushing: " + lowestDegreeNode);
            stack.push(lowestDegreeNode);

        } while (true);

        List<InterferenceGraph.InterNode> toSpill = spillingNodes(interferenceGraph);

        for (InterferenceGraph.InterNode node : toSpill) {
            node.takeFromGraph();
            stack.push(node);
        }
        System.out.println("stack: " + stack);
        System.out.println("interferenceGraph: " + interferenceGraph);

        int colorsUsed = colorNodes(stack);

        if (wantedColors > 0 && colorsUsed > wantedColors) {
            throw new RuntimeException("Impossible to color with " + wantedColors + " registers. Need " + colorsUsed + " registers.");
        }

        //TODO : spilling


        return colorsUsed;
    }

    public List<GraphNode> liveliness(Method method) {

        method.buildCFG();

        List<GraphNode> nodes = GraphNode.getFromNodes(method.getInstructions());

        // Collections.reverse(nodes);

        boolean end;
        do {
            end = true;
            for (GraphNode node : nodes) {

                Set<Element> inL = new HashSet<>(node.getIn()); // in'
                Set<Element> outL = new HashSet<>(node.getOut()); // out'


                Set<Element> use = new HashSet<>(node.getUse());
                Set<Element> def = new HashSet<>(node.getDef());

                // in[n] = use[n] U (out[n] - def[n])
                Set<Element> diff = new HashSet<>(node.getOut());

                System.out.println("======================\n");
                System.out.println("diff: " + diff);
                System.out.println("def: " + def);
                //diff.removeAll(def);
                for (Element element : def) {
                    for (Element element1 : diff) {
                        if (element1.toString().equals(element.toString())) {
                            diff.remove(element1);
                            break;
                        }
                    }
                }
                System.out.println("Result: " + diff);
                System.out.println("======================\n");


                use.addAll(diff);

                node.setIn(use);

                Set<Element> outAux = new HashSet<>();
                for (GraphNode successor : node.getSuccessors()) {
                    outAux.addAll(successor.getIn());
                }
                node.setOut(outAux);

               if (!(inL.equals(node.getIn()) && outL.equals(node.getOut()))) {
                    end = false;
                }
            }

        } while (!end);

        for (GraphNode node : nodes) {
            try {
                System.out.println(node.getInstruction());
                System.out.println(node.getInstruction().getClass());
                System.out.println("in: " + node.getIn());
                System.out.println("out: " + node.getOut());
                System.out.println("use: " + node.getUse());
                System.out.println("def: " + node.getDef());
                System.out.println();
            } catch (Exception ignored) {

            }
        }


        return nodes;

    }
}
