package edu.kit.kastel.vads.compiler.ir.node;

public final class EqualNode extends BinaryOperationNode {

    EqualNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
