package edu.kit.kastel.vads.compiler.ir.node;

public final class EqualNode extends BinaryOperationNode {

    public EqualNode(Block block, Node left, Node right, int size) {
        super(block, left, right);
    }
    public EqualNode(Block block, Node left, Node right) {
        this(block, left, right, 1);
    }
}
