package edu.kit.kastel.vads.compiler.backend;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;

import java.util.*;
import java.util.stream.Collectors;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class LivenessAnalysis {
private final Set<Node> liveIn;

private final Map<Node, Set<Node>> liveNodes;

    private final Map<IrGraph, List<Node>> controlFlowOrders;
    public LivenessAnalysis() {
        liveIn= new HashSet<>();

        liveNodes = new HashMap<>();
        controlFlowOrders = new HashMap<>();
    }

    /**
     * Analyzes the liveness of variables in the given IR graph.
     *
     * @param irGraph The IR graph to analyze.
     */
    public void analyzeLiveness(IrGraph irGraph) {
        Node node = irGraph.endBlock();
            // Perform liveness analysis for each block
        controlFlowOrders.put(irGraph ,irGraph.getControlFlowOrder());

        for(Node pre: node.predecessors()) {
            getLivenessOfNode(pre, liveIn, liveNodes);
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

    public void getLivenessOfNode(Node node, Set<Node> visited, Map<Node, Set<Node>> liveNodes) {

        if (visited.contains(node)) {
            return;
        }
        visited.add(node);

        // calc liveOut
        Set<Node> liveOut = getLiveOut(node, successors(node).stream().map(liveNodes::get).collect(Collectors.toList()));


//TODO: add liveness analysis with logic base from lecture properly
        if (liveNodes.containsKey(node) && liveNodes.get(node).equals(liveIn)) {
            return;
        }
        liveNodes.put(node, liveIn);
        for (Node successor : successors(node)) {
            Set<Node> successorLiveIn = liveNodes.getOrDefault(successor, new HashSet<>());
            liveOut.addAll(successorLiveIn);
        }
        // compute liveIn
        Set<Node> liveIn = new HashSet<>(getUses(node));
        liveOut.removeAll(getDefs(node));
        liveIn.addAll(liveOut);

        for (Node predecessor : node.predecessors()) {
            getLivenessOfNode(predecessor, visited, liveNodes);
        }


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
            uses.add(predecessorSkipProj(node, BinaryOperationNode.LEFT));
            uses.add(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        } else if (node instanceof ReturnNode){
            uses.add(predecessorSkipProj(node, ReturnNode.RESULT));
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
