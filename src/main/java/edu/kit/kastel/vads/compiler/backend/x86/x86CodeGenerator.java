package edu.kit.kastel.vads.compiler.backend.x86;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class x86CodeGenerator {

    public String generateCode(List<IrGraph> program) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(prologue());
        for (IrGraph graph : program) {
            x86RegisterAllocator allocator = new x86RegisterAllocator(graph);
            Map<Node, Register> registers = allocator.allocateRegisters(graph);
//
//            try {
//                FileWriter file = new FileWriter("graph.vcg", true);
//                FileWriter registerfile = new FileWriter("register.txt", true);
//                file.write(GraphVizPrinter.print(graph));
//
//                registerfile.write(graph.name()+":\n");
//                registerfile.write(registers.keySet().stream()
//                        .map(key -> key + "=" + registers.get(key)+"\n")
//                        .collect(Collectors.joining(", ", "{", "}")));
//                file.close();
//                registerfile.close();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }

            if (graph.name().equals("main")) {
                builder.append(generatePrologue("_main"));
                generateForGraph(graph, builder, registers);
                generateEpilogue("_main", builder);
            }else{
                builder.append(graph.name())
                        .append(": \n");
                generateForGraph(graph, builder, registers);
                generateEpilogue(graph.name(), builder);
            }


        }
        FileWriter writer = new FileWriter("output.s");
        writer.write(builder.toString());
        writer.close();
        return builder.toString();
    }
    public void generateEpilogue(String name, StringBuilder builder){
        builder.append( "# end of " +name+ "\n");
    }
    public String prologue(){
        return ".intel_syntax noprefix" + "\n" +
                ".global main\n" +
                ".global _main\n" +
                ".text\n\n" +
                "main:\n" +
                "  call _main\n" +
                "\n" +
                "  mov " + x86Registers.RealRegisters.EDI + ", " + x86Registers.RealRegisters.EAX + "\n" +
                "  mov " + x86Registers.RealRegisters.EAX + " , 0x3c\n" +
                "  syscall\n\n";
    }
    public  String generatePrologue(String name){
        if (!name.equals("_main"))
        return ".global "+ name+ "\n"+ name+":\n";
        else return name+":\n";
    }

    private void generateForGraph(IrGraph graph, StringBuilder builder, Map<Node, Register> registers) {
//        Set<Node> visited = new HashSet<>();
//        scanfornodes(graph.endBlock(), visited, builder, registers);
//    }
//
//    private void scanfornodes(Node node, Set<Node> visited, StringBuilder builder, Map<Node, Register> registers) {
//        for (Node predecessor : node.predecessors()) {
//            if (!visited.contains(predecessor)) {
//                visited.add(predecessor);
//                scanfornodes(predecessor, visited, builder, registers);
//
//            }
//        }
        for (Node node : graph.getControlFlowOrder()) {

            switch (node) {
                case AddNode add -> binary(builder, registers, add, "add");
                case SubNode sub -> subtract(builder, registers, sub, "sub");
                case MulNode mul -> binary(builder, registers, mul, "imul");
                case DivNode div -> {
                    builder.append("mov ").append(x86Registers.RealRegisters.EAX).append(", ")
                            .append(registers.get(predecessorSkipProj(div, BinaryOperationNode.LEFT))).append("\n");
                    builder.append("cdq\n");
                    builder.append("idiv ").append(registers.get(predecessorSkipProj(div, BinaryOperationNode.RIGHT))).append("\n");
                }
                case ModNode mod -> builder.append("mov ").append(x86Registers.RealRegisters.EAX).append(", ")
                        .append(registers.get(predecessorSkipProj(mod, BinaryOperationNode.LEFT))).append("\n")
                        .append("cdq\n")
                        .append("idiv ").append(registers.get(predecessorSkipProj(mod, BinaryOperationNode.RIGHT))).append("\n")
                        .append("mov ").append(registers.get(mod)).append(", ").append(x86Registers.RealRegisters.EDX).append("\n");
                case ReturnNode r -> builder.repeat(" ", 2)
                        .append("mov ").append(x86Registers.RealRegisters.EAX)
                        .append(", ")
                        .append(registers.get(predecessorSkipProj(r, ReturnNode.RESULT)))
                        .append(System.lineSeparator())
                        .append("  ret\n");
                case ConstIntNode c -> builder.append("mov ").append(registers.get(c)).append(", ").append(c.value()).append("\n");
                case ConstBoolNode c -> builder.append("mov ").append(registers.get(c)).append(", ").append(c.value() ? 1 : 0).append("\n");
                case Phi _ -> throw new UnsupportedOperationException("phi");
                case Block _, ProjNode _, StartNode _ -> {
                    // do nothing, skip line break
                }
                case ArithmeticShiftLeftNode arithmeticShiftLeftNode -> {
                    Register target = registers.get(arithmeticShiftLeftNode);
                    Register left = registers.get(predecessorSkipProj(arithmeticShiftLeftNode, BinaryOperationNode.LEFT));
                    Register right = registers.get(predecessorSkipProj(arithmeticShiftLeftNode, BinaryOperationNode.RIGHT));
                    if (left instanceof x86Registers.OverflowRegisters){
                        builder.append("  ")
                                .append("mov ")
                                .append(x86Registers.RealRegisters.R15D).append(", ")
                                .append(left).append("\n");
                        builder.append("sal ").append(x86Registers.RealRegisters.R15D).append(", ").append(right).append("\n")
                                .append("mov ").append(target).append(", ").append(x86Registers.RealRegisters.R15D).append("\n");
                    } else {
                        builder.append("  ")
                                .append("sal ").append(target).append(", ").append(right).append("\n");
                    }
                }
                case ArithmeticShiftRightNode arithmeticShiftRightNode -> {
                    Register target = registers.get(arithmeticShiftRightNode);
                    Register left = registers.get(predecessorSkipProj(arithmeticShiftRightNode, BinaryOperationNode.LEFT));
                    Register right = registers.get(predecessorSkipProj(arithmeticShiftRightNode, BinaryOperationNode.RIGHT));
                    if (left instanceof x86Registers.OverflowRegisters){
                        builder.append("  ")
                                .append("mov ")
                                .append(x86Registers.RealRegisters.R15D).append(", ")
                                .append(left).append("\n");
                        builder.append("sar ").append(x86Registers.RealRegisters.R15D).append(", ").append(right).append("\n")
                                .append("mov ").append(target).append(", ").append(x86Registers.RealRegisters.R15D).append("\n");
                    } else {
                        builder.append("  ")
                                .append("sar ").append(target).append(", ").append(right).append("\n");
                    }
                }
                case BitwiseAndNode bitwiseAndNode -> {
                    binary(builder, registers, bitwiseAndNode, "and");
                }
                case BitwiseOrNode bitwiseOrNode -> {
                    binary(builder, registers, bitwiseOrNode, "or");
                }
                case LogicalAndNode logicalAndNode -> {
                    binary( builder, registers, logicalAndNode, "and");
 //                    Register target = registers.get(logicalAndNode);
//                    Register left = registers.get(predecessorSkipProj(logicalAndNode, BinaryOperationNode.LEFT));
//                    Register right = registers.get(predecessorSkipProj(logicalAndNode, BinaryOperationNode.RIGHT));
//                    builder.append("  ")
//                        .append("mov ").append(target).append(", ").append(left).append("\n")
//                        .append("  ")
//                        .append("and ").append(target).append(", ").append(right).append("\n");
                }
                case LogicalOrNode logicalOrNode -> {
                    binary( builder, registers, logicalOrNode, "or");
//                    Register target = registers.get(logicalOrNode);
//                    Register left = registers.get(predecessorSkipProj(logicalOrNode, BinaryOperationNode.LEFT));
//                    Register right = registers.get(predecessorSkipProj(logicalOrNode, BinaryOperationNode.RIGHT));
//                    builder.append("  ")
//                        .append("mov ").append(target).append(", ").append(left).append("\n")
//                        .append("  ")
//                        .append("or ").append(target).append(", ").append(right).append("\n");
                }
                case LogicalShiftLeftNode logicalShiftLeftNode -> {
                    Register target = registers.get(logicalShiftLeftNode);
                    Register left = registers.get(predecessorSkipProj(logicalShiftLeftNode, BinaryOperationNode.LEFT));
                    Register right = registers.get(predecessorSkipProj(logicalShiftLeftNode, BinaryOperationNode.RIGHT));
                    if (left instanceof x86Registers.OverflowRegisters){
                        builder.append("  ")
                                .append("mov ")
                                .append(x86Registers.RealRegisters.R15D).append(", ")
                                .append(left).append("\n");
                        builder.append("shl ").append(x86Registers.RealRegisters.R15D).append(", ").append(right).append("\n")
                                .append("mov ").append(target).append(", ").append(x86Registers.RealRegisters.R15D).append("\n");
                    } else {
                        builder.append("  ")
                                .append("shl ").append(target).append(", ").append(right).append("\n");
                    }
                }
                case LogicalShiftRightNode logicalShiftRightNode -> {
                    Register target = registers.get(logicalShiftRightNode);
                    Register left = registers.get(predecessorSkipProj(logicalShiftRightNode, BinaryOperationNode.LEFT));
                    Register right = registers.get(predecessorSkipProj(logicalShiftRightNode, BinaryOperationNode.RIGHT));
                    if (left instanceof x86Registers.OverflowRegisters){
                        builder.append("  ")
                                .append("mov ")
                                .append(x86Registers.RealRegisters.R15D).append(", ")
                                .append(left).append("\n");
                        builder.append("shr ").append(x86Registers.RealRegisters.R15D).append(", ").append(right).append("\n")
                                .append("mov ").append(target).append(", ").append(x86Registers.RealRegisters.R15D).append("\n");
                    } else {
                        builder.append("  ")
                                .append("shr ").append(target).append(", ").append(right).append("\n");
                    }
                }
                case XorNode xorNode -> {
                    Register target = registers.get(xorNode);
                    Register left = registers.get(predecessorSkipProj(xorNode, BinaryOperationNode.LEFT));
                    Register right = registers.get(predecessorSkipProj(xorNode, BinaryOperationNode.RIGHT));
                    if (left instanceof x86Registers.OverflowRegisters&& right instanceof x86Registers.OverflowRegisters){
                        builder.append("  ")
                                .append("mov ")
                                .append(x86Registers.RealRegisters.R15D).append(", ")
                                .append(left).append("\n")
                                .append("xor ").append(x86Registers.RealRegisters.R15D).append(", ").append(right).append("\n")
                                .append("mov ").append(target).append(", ").append(x86Registers.RealRegisters.R15D).append("\n");
                    } else if (right instanceof x86Registers.OverflowRegisters&& right.equals(target)){
                        builder.append("  ")
                                .append("mov ")
                                .append(x86Registers.RealRegisters.R15D).append(", ")
                                .append(right).append("\n")
                                .append("xor ").append(x86Registers.RealRegisters.R15D).append(", ").append(left).append("\n")
                                .append("mov ").append(target).append(", ").append(x86Registers.RealRegisters.R15D).append("\n");
                    } else {
                        builder.append("  ")
                            .append("xor ").append(target).append(", ").append(left).append("\n")
                            .append("  ")
                            .append("xor ").append(target).append(", ").append(right).append("\n");
                    }
                }
                case EqualNode equal -> {
                    Register target = registers.get(equal);
                    Register left = registers.get(predecessorSkipProj(equal, BinaryOperationNode.LEFT));
                    Register right = registers.get(predecessorSkipProj(equal, BinaryOperationNode.RIGHT));
                    builder.append("  ")
                        .append("mov ").append(target).append(", ").append(left).append("\n")
                        .append("  ")
                        .append("cmp ").append(target).append(", ").append(right).append("\n")
                        .append("  ")
                        .append("sete ").append(target).append("\n");
                }
                case NotEqualNode notEqual -> {
                    Register target = registers.get(notEqual);
                    Register left = registers.get(predecessorSkipProj(notEqual, BinaryOperationNode.LEFT));
                    Register right = registers.get(predecessorSkipProj(notEqual, BinaryOperationNode.RIGHT));
                    builder.append("  ")
                        .append("mov ").append(target).append(", ").append(left).append("\n")
                        .append("  ")
                        .append("cmp ").append(target).append(", ").append(right).append("\n")
                        .append("  ")
                        .append("setne ").append(target).append("\n");
                }
                case LessThanNode lessThan -> {
                    Register target = registers.get(lessThan);
                    Register left = registers.get(predecessorSkipProj(lessThan, BinaryOperationNode.LEFT));
                    Register right = registers.get(predecessorSkipProj(lessThan, BinaryOperationNode.RIGHT));
                    builder.append("  ")
                        .append("mov ").append(target).append(", ").append(left).append("\n")
                        .append("  ")
                        .append("cmp ").append(target).append(", ").append(right).append("\n")
                        .append("  ")
                        .append("setl ").append(target).append("\n");
                }
                case LessThanOrEqualNode lessThanOrEqual -> {
                    Register target = registers.get(lessThanOrEqual);
                    Register left = registers.get(predecessorSkipProj(lessThanOrEqual, BinaryOperationNode.LEFT));
                    Register right = registers.get(predecessorSkipProj(lessThanOrEqual, BinaryOperationNode.RIGHT));
                    builder.append("  ")
                        .append("mov ").append(target).append(", ").append(left).append("\n")
                        .append("  ")
                        .append("cmp ").append(target).append(", ").append(right).append("\n")
                        .append("  ")
                        .append("setle ").append(target).append("\n");
                }
                case JumpNode jump -> {
                    Register target = registers.get(jump);
                    builder.append("  ")
                        .append("jmp ").append(target).append("\n");
                }
                case IfNode ifNode -> {
                    Register condition = registers.get(ifNode);
                    builder.append("  ")
                        .append("cmp ").append(condition).append(", 0\n")
                        .append("  ")
                        .append("je ").append("elseBlock").append("\n")
                        .append("  ")
                        .append("jmp ").append("thenBlock").append("\n");
                }
                default -> {
                    builder.append("errror : not implemented node type: ")
                        .append(node.getClass().getSimpleName())
                        .append("\n");
            }
            }

        }

        builder.append("\n");
    }

  private static void binary(
      StringBuilder builder,
      Map<Node, Register> registers,
      BinaryOperationNode node,
      String opcode
      ) {
        Register target = registers.get(node);
        Register left = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register right = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        if (left instanceof x86Registers.OverflowRegisters&& right instanceof x86Registers.OverflowRegisters){
            builder.append("  ")
                .append("mov ")
                .append(x86Registers.RealRegisters.R15D).append(", ")
                .append(left).append("\n")
                    .append(opcode).append(" ").append(x86Registers.RealRegisters.R15D).append(", ").append(right).append("\n")
                    .append("mov ").append(target).append(", ").append(x86Registers.RealRegisters.R15D).append("\n");
        } else if (right instanceof x86Registers.OverflowRegisters&& right.equals(target)){
            builder.append("  ")
                .append("mov ")
                .append(x86Registers.RealRegisters.R15D).append(", ")
                .append(right).append("\n")
                    .append(opcode).append(" ").append(x86Registers.RealRegisters.R15D).append(", ").append(left).append("\n")
                    .append("mov ").append(target).append(", ").append(x86Registers.RealRegisters.R15D).append("\n");
        }

      else if (target.equals(left)) {
            builder.append("  ")
                .append(opcode).append(" ")
                .append(target).append(", ")
                .append(right).append("\n");

        } else if (target.equals(right)) {
            builder.append("  ")
                .append(opcode).append(" ")
                .append(target).append(", ")
                .append(left).append("\n");
        } else {
        builder.repeat(" ", 2)
            .append("mov ")
            .append(target).append(", ")
            .append(left).append("\n  ")
            .append(opcode).append(" ")
            .append(target).append(", ")
        .append(right).append("\n");
  }
    }

    private static void subtract(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String opcode
            ) {
        Register target = registers.get(node);
        Register left = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register right = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        if (left instanceof x86Registers.OverflowRegisters){
            builder.append("  ")
                    .append("mov ")
                    .append(x86Registers.RealRegisters.R15D).append(", ")
                    .append(left).append("\n");
        }
        if (target.equals(left)) {
            builder.append("  ")
                    .append(opcode).append(" ")
                    .append(target).append(", ")
                    .append(right).append("\n");
            return;

        }
        builder.repeat(" ", 2)
                .append("mov ")
                .append(target).append(", ")
                .append(left).append("\n  ")
                .append(opcode).append(" ")
                .append(target).append(", ")
                .append(right).append("\n");
    }
}
