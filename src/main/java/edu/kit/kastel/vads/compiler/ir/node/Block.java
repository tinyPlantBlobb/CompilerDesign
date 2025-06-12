package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public final class Block extends Node {
    private final String identifier ;
    public Block(IrGraph graph) {
        super(graph);
        identifier = "Block";
    }

    public  Block(IrGraph graph, String in) {
        super(graph);
        this.identifier = "Block " +in;
    }

}
