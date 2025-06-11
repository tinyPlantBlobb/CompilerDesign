package edu.kit.kastel.vads.compiler.ir.node;

public final class LogicalOrNode extends BinaryOperationNode {

    private LogicalOrNode(Block block, Node left, Node right) {
        super(block, left, right);
    }

}
