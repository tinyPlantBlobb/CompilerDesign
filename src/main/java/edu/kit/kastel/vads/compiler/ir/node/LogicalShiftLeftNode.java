package edu.kit.kastel.vads.compiler.ir.node;

public final class LogicalShiftLeftNode extends BinaryOperationNode {
    LogicalShiftLeftNode(Block block, Node left, Node right, Node sideEffect) {
        super(block, left, right, sideEffect);
    }
}
