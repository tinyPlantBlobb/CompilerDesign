package edu.kit.kastel.vads.compiler.ir.node;

public final class ArithmeticShiftRightNode extends BinaryOperationNode {
    ArithmeticShiftRightNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
