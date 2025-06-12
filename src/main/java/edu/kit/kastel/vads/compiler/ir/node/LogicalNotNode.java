package edu.kit.kastel.vads.compiler.ir.node;

public final class LogicalNotNode extends Node {
    public LogicalNotNode(Block block, Node operand) {
        super(block, operand);
    }
}
