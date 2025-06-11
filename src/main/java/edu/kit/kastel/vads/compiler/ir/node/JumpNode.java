package edu.kit.kastel.vads.compiler.ir.node;

public final class JumpNode extends Node {
    private final Block targetBlock;

    public JumpNode(Block block, Block targetBlock) {
        super(block);
        this.targetBlock = targetBlock;
    }


}
