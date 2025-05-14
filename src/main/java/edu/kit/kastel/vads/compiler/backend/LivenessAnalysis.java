package edu.kit.kastel.vads.compiler.backend;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;

import java.util.*;

public class LivenessAnalysis {
private final Set<Node> liveIn;
private final Set<Node> liveOut;
private final Map<Node, Set<Node>> liveNodes;

    public LivenessAnalysis() {
        liveIn= new HashSet<>();
        liveOut= new HashSet<>();
        liveNodes = new HashMap<>();
    }

    /**
     * Analyzes the liveness of variables in the given IR graph.
     *
     * @param irGraph The IR graph to analyze.
     */
    public void analyzeLiveness(IrGraph irGraph) {
        Node node = irGraph.endBlock();
            // Perform liveness analysis for each block
        for(Node pre: node.predecessors()) {
            getLivenessOfNode(pre, liveIn, liveOut, liveNodes);
        }
    }
    public void getLivenessOfNode(Node node, Set<Node> liveIn, Set<Node> liveOut, Map<Node, Set<Node>> liveNodes) {

        Set<Node> uses = getUses(node);
        Set<Node> defs = getDefs(node);
        Set<Node> in = new HashSet<>(uses);
        Set<Node> out = new HashSet<>();
//TODO: add liveness analysis with logic base from lecture properly

        for (Node predecessor : node.predecessors()) {
            getLivenessOfNode(predecessor, liveIn, liveOut, liveNodes);

            out.addAll(liveOut);
        }
        in.addAll(out);
        in.removeAll(defs);
        liveNodes.put(node, in);
        liveIn.addAll(in);
        liveOut.addAll(out);

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
