package edu.kit.kastel.vads.compiler.ir.node;

public final class GreaterThanOrEqualNode extends BinaryOperationNode {
    GreaterThanOrEqualNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
