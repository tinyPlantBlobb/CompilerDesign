package edu.kit.kastel.vads.compiler.ir.node;

public final class GreaterThanNode extends BinaryOperationNode {
    GreaterThanNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
