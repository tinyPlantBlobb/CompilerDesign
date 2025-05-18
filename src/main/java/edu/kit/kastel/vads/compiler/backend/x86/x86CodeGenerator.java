package edu.kit.kastel.vads.compiler.backend.x86;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;
import edu.kit.kastel.vads.compiler.ir.util.GraphVizPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class x86CodeGenerator {

    public String generateCode(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();
        builder.append(prologue());
        for (IrGraph graph : program) {
            x86RegisterAllocator allocator = new x86RegisterAllocator(graph);
            Map<Node, Register> registers = allocator.allocateRegisters(graph);

            try {
                FileWriter file = new FileWriter("graph.vcg", true);
                FileWriter registerfile = new FileWriter("register.txt", true);
                file.write(GraphVizPrinter.print(graph));
                System.out.println(registers.keySet().stream()
                        .map(key -> key + "=" + registers.get(key))
                        .collect(Collectors.joining(", ", "{", "}")));
                registerfile.write(graph.name()+":\n");
                registerfile.write(registers.keySet().stream()
                        .map(key -> key + "=" + registers.get(key))
                        .collect(Collectors.joining(", ", "{", "}\n ")));
                file.close();
                registerfile.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (graph.name().equals("main")) {
                builder.append(generatePrologue("main_"));
                generateForGraph(graph, builder, registers);
            }else{
                builder.append(graph.name())
                        .append(": \n");
                generateForGraph(graph, builder, registers);
            }


        }
        System.out.println(builder);
        return builder.toString();
    }
    public String prologue(){
        return ".intel_syntax noprefix" + "\n" +
                ".global main\n" +
                ".global _main\n" +
                ".text\n\n" +
                "main:\n" +
                "call _main\n" +
                "\n" +
                "mov " + x86Registers.RealRegisters.RDI + ", " + x86Registers.RealRegisters.RAX + "\n" +
                "mov " + x86Registers.RealRegisters.RAX + " , 0x3c\n" +
                "syscall\n\n";
    }
    public  String generatePrologue(String name){
        return ".global "+ name+ "\n"+ name+":\n";
    }

    private void generateForGraph(IrGraph graph, StringBuilder builder, Map<Node, Register> registers) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited, builder, registers);
    }

    private void scan(Node node, Set<Node> visited, StringBuilder builder, Map<Node, Register> registers) {
        for (Node predecessor : node.predecessors()) {
            if (!visited.contains(predecessor)) {
                visited.add(predecessor);
                scan(predecessor, visited, builder, registers);
            }
        }

        switch (node) {
            case AddNode add -> binary(builder, registers, add, "add");
            case SubNode sub -> binary(builder, registers, sub, "sub");
            case MulNode mul -> binary(builder, registers, mul, "mul");
            case DivNode div -> {
                builder.append("mov ").append(x86Registers.RealRegisters.RAX).append(", ")
                        .append(registers.get(predecessorSkipProj(div, BinaryOperationNode.LEFT))).append("\n");
                builder.append("cqo\n"); // Sign extension for division
                builder.append("idiv ").append(registers.get(predecessorSkipProj(div, BinaryOperationNode.RIGHT))).append("\n");
            }
            case ModNode mod -> builder.append("mov ").append(x86Registers.RealRegisters.RAX).append(", ")
                    .append(registers.get(predecessorSkipProj(mod, BinaryOperationNode.LEFT))).append("\n")
                    .append("cqo\n")
                    .append("idiv ").append(registers.get(predecessorSkipProj(mod, BinaryOperationNode.RIGHT))).append("\n")
                    .append("mov ").append(registers.get(mod)).append(", ").append(x86Registers.RealRegisters.RDX).append("\n");
            case ReturnNode r -> builder.repeat(" ", 2)
                    .append("mov ").append(x86Registers.RealRegisters.RAX)
                    .append(", ")
                    .append(registers.get(predecessorSkipProj(r, ReturnNode.RESULT)))
                    .append(System.lineSeparator())
                    .append("ret\n");
            case ConstIntNode c -> builder.repeat(" ", 2)
                    .append("mov ")
                    .append(registers.get(c)).append(", ")
                    .append(c.value());
            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
        builder.append("\n");
    }

  private static void binary(
      StringBuilder builder,
      Map<Node, Register> registers,
      BinaryOperationNode node,
      String opcode) {
        Register target = registers.get(node);
        Register left = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register right = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
    builder.repeat(" ", 2)
            .append("mov ")
            .append(target).append(", ")
            .append(left).append("\n  ")
            .append(opcode).append(" ")
            .append(target).append(", ")
        .append(right).append("\n");
  }
}
