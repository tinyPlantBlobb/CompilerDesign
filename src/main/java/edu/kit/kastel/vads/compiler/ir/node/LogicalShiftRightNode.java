package edu.kit.kastel.vads.compiler.ir.node;

public final class LogicalShiftRightNode extends BinaryOperationNode {
     LogicalShiftRightNode(Block block, Node left, Node right, Node sideEffect) {
        super(block, left, right, sideEffect);
    }
}
