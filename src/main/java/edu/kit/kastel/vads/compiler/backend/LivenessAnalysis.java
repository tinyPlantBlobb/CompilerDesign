package edu.kit.kastel.vads.compiler.backend;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;

import java.util.*;
import java.util.stream.Collectors;

public class LivenessAnalysis {
private final Set<Node> liveIn;
private final Set<Node> liveOut;
private final Map<Node, Set<Node>> liveNodes;
private List<Node> controlFlowGraph;
    public LivenessAnalysis() {
        liveIn= new HashSet<>();
        liveOut= new HashSet<>();
        liveNodes = new HashMap<>();
        controlFlowGraph = new ArrayList<>();  }

    /**
     * Analyzes the liveness of variables in the given IR graph.
     *
     * @param irGraph The IR graph to analyze.
     */
    public void analyzeLiveness(IrGraph irGraph) {
        Node node = irGraph.endBlock();
            // Perform liveness analysis for each block
        controlFlowGraph= irGraph.getControlFlowOrder();

        for(Node pre: node.predecessors()) {
            getLivenessOfNode(pre, liveIn, liveOut, liveNodes);
        }
    }
    public List<Node> successors(Node node) {
        List<Node> successors = new ArrayList<>();
        for (Node potentialSuccessor : liveNodes.keySet()) {
            if (potentialSuccessor.predecessors().contains(node)) {
                successors.add(potentialSuccessor);
            }
        }
        return successors;
    }

    public void getLivenessOfNode(Node node, Set<Node> liveIn, Set<Node> visited, Map<Node, Set<Node>> liveNodes) {
        Map<Node, Set<Node>> inValues = new HashMap<>();
        Set<Node> uses = getUses(node);
        Set<Node> defs = getDefs(node);
        visited.add(node);
        List<Set<Node>> inputs = successors(node).stream().map(inValues::get).filter(Objects::nonNull)
                .collect(Collectors.toList());
//TODO: add liveness analysis with logic base from lecture properly

        for (Node predecessor : node.predecessors()) {
            if(!visited.contains(predecessor)) {
            liveIn.addAll(uses);
            liveIn.removeAll(defs);
            getLivenessOfNode(predecessor, liveIn, visited, liveNodes);
          //liveIn.addAll(getUses(predecessor));
          //liveIn.removeAll(getDefs(predecessor));
        }
        }
        liveIn.removeAll(defs);
        liveNodes.put(node, liveIn);


    }
    private Set<Node> getLiveIn(Node node, Set<Node> liveOut) {
        Set<Node> defs = getDefs(node);
        Set<Node> uses = getUses(node);
        Set<Node> liveIn = new HashSet<>(uses);
        liveOut.removeAll(defs);
        liveIn.addAll(liveOut);
        return liveIn;
    }
    private Set<Node> getLiveOut(Node node, List<Set<Node>> inputs) {
    return inputs.stream()
            .filter(Objects::nonNull)
            .reduce(new HashSet<>(), (a, b) -> {
                Set<Node> union = new HashSet<>(a);
                union.addAll(b);
                return union;
            });
    }

    private Set<Node> getUses(Node node) {
        Set<Node> uses = new HashSet<>();
        if (node instanceof BinaryOperationNode){
            uses.add(node.predecessors().get(0));
            uses.add(node.predecessors().get(1));
        } else if (node instanceof ReturnNode){
            uses.add(node.predecessors().get(1));
        }

        return uses;
    }

    private Set<Node> getDefs(Node node) {
        Set<Node> defs = new HashSet<>();
        if ( node instanceof ConstIntNode || node instanceof BinaryOperationNode) {

            defs.add(node);
        }
        return defs;
    }
}
