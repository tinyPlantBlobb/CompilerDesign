package edu.kit.kastel.vads.compiler.ir.node;

public final class NotEqualNode extends BinaryOperationNode {
    NotEqualNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
