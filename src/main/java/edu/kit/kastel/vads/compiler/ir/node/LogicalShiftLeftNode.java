package edu.kit.kastel.vads.compiler.ir.node;

public final class LogicalShiftLeftNode extends BinaryOperationNode {
    public LogicalShiftLeftNode(Block block, Node left, Node right, Node sideEffect) {
        super(block, left, right, sideEffect);
    }
}
