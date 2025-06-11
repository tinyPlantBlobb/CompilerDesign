package edu.kit.kastel.vads.compiler.ir.node;

public final class LogicalAndNode extends BinaryOperationNode {
    LogicalAndNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
