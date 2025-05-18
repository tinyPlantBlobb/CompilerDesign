package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.LivenessAnalysis;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.backend.aasm.VirtualRegister;

import java.util.*;

public class x86RegisterAllocator implements RegisterAllocator {
    private int id = 0;
    private final Map<Node, Register> registers = new HashMap<>();
    private final Deque<x86Registers> availableRegisters = new ArrayDeque<>();
    private final LivenessAnalysis analysis;

    public x86RegisterAllocator(IrGraph graph) {
        // Initialize the available registers
        for (x86Registers.RealRegisters reg : x86Registers.RealRegisters.values()) {
            if (reg != x86Registers.RealRegisters.RSP && reg != x86Registers.RealRegisters.RBP&&reg
                    !=x86Registers.RealRegisters.EAX&&reg!=x86Registers.RealRegisters.EDX&&reg!=x86Registers.RealRegisters.R15D) { // Stack- und Frame-Pointer auslassen
                availableRegisters.add(reg);
            }

        }
        this.analysis = new LivenessAnalysis();
        this.analysis.analyzeLiveness(graph);
    }
    @Override
    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        Set<Node> visited = new HashSet<>();
        visited.add(graph.endBlock());
        scan(graph.endBlock(), visited, new ArrayList<>());
        return Map.copyOf(this.registers);
    }

    private void scan(Node node, Set<Node> visited, List<Node> controlflow) {
        /*
        * add liveness analysis with backwards look by adding
        * in a set of nodes and when the node numebr has to be live
        * add when node is defines, when used and thus when it is live
        */

        for (Node predecessor : node.predecessors()) {
            if (!visited.contains(predecessor)) {
                scan(predecessor, visited, controlflow);
            }
        }

        controlflow.add(node);
        if (needsRegister(node)) {
            if (!availableRegisters.isEmpty()) {
                    this.registers.put(node, availableRegisters.pop());
            }
            else {
                this.registers.put(node, new x86Registers.OverflowRegisters(this.id++));
            }
        }
    }

    private static boolean needsRegister(Node node) {
        // TODO: add liveness analysis/needed analysis
        // TODO: add register freeing and const propagation (|| node instanceof ConstIntNode)
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block );
    }

}
