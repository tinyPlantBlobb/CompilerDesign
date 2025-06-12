package edu.kit.kastel.vads.compiler.ir.node;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public final class IfNode extends Node {

    public IfNode(Block block, Node condition) {
        super(block, condition);
    }
    private final int condition = 0;

    public Node getCondition() {
        return predecessorSkipProj(this, condition);
    }
}
