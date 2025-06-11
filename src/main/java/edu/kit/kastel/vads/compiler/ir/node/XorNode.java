package edu.kit.kastel.vads.compiler.ir.node;

public final class XorNode extends BinaryOperationNode {
    XorNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
