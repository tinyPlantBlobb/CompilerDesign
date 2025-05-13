package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.backend.aasm.VirtualRegister;

import java.util.*;

public class x86RegisterAllocator implements RegisterAllocator {
    private int id;
    private final Map<Node, Register> registers = new HashMap<>();
    private final Deque<x86Registers> availableRegisters = new ArrayDeque<>();

    public x86RegisterAllocator() {
        // Initialize the available registers
        for (x86Registers reg : x86Registers.values()) {
            if (reg != x86Registers.RSP && reg != x86Registers.RBP&&reg!=x86Registers.RAX&&reg!=x86Registers.RDX&&reg!=x86Registers.R15) { // Stack- und Frame-Pointer auslassen
                availableRegisters.add(reg);
            }
        }
    }
    @Override
    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        Set<Node> visited = new HashSet<>();
        visited.add(graph.endBlock());
        scan(graph.endBlock(), visited);
        return Map.copyOf(this.registers);
    }

    private void scan(Node node, Set<Node> visited) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited);
            }
        }
        if (needsRegister(node)) {
            if (!availableRegisters.isEmpty()) {
                    this.registers.put(node, availableRegisters.pop());
            }
            this.registers.put(node, new VirtualRegister(this.id++));
        }
    }

    private static boolean needsRegister(Node node) {
        // TODO: add liveness analysis/needed analysis
        // TODO: add register freeing and const propagation (|| node instanceof ConstIntNode)
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block || node instanceof ReturnNode );
    }
}
