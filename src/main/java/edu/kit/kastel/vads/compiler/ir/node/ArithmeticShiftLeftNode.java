package edu.kit.kastel.vads.compiler.ir.node;

public final class ArithmeticShiftLeftNode extends BinaryOperationNode {
   ArithmeticShiftLeftNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
