package edu.kit.kastel.vads.compiler.backend.x86;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.ir.util.YCompPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class x86CodeGenerator {

    public String generateCode(List<IrGraph> program) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(prologue());

        FileWriter file = new FileWriter("graphs/test1.vcg", false);

        for (IrGraph graph : program) {
            x86RegisterAllocator allocator = new x86RegisterAllocator(graph);
            Map<Node, Register> registers = allocator.allocateRegisters(graph);

            try {
                file.write(YCompPrinter.print(graph));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

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
        System.out.println(builder.toString());
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
        Node current = graph.startBlock();
        for (Node node : graph.getControlFlowOrder()) {
            System.out.println(node.getClass() + " " + node.hashCode() + node.block() + " " + node.predecessors());
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
                case ArithmeticShiftLeftNode arithmeticShiftLeftNode -> shiftNode(builder, registers, arithmeticShiftLeftNode, "sal");
                case ArithmeticShiftRightNode arithmeticShiftRightNode -> shiftNode(builder, registers, arithmeticShiftRightNode, "sar");
                case BitwiseAndNode bitwiseAndNode -> binary(builder, registers, bitwiseAndNode, "and");
                case BitwiseOrNode bitwiseOrNode -> binary(builder, registers, bitwiseOrNode, "or");
                case LogicalAndNode logicalAndNode -> binary( builder, registers, logicalAndNode, "and");
                case LogicalOrNode logicalOrNode -> binary( builder, registers, logicalOrNode, "or");
                case LogicalShiftLeftNode logicalShiftLeftNode -> shiftNode(builder, registers, logicalShiftLeftNode, "shl");
                case LogicalShiftRightNode logicalShiftRightNode -> shiftNode(builder, registers, logicalShiftRightNode, "shr");
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
                    System.out.println("append jump: " + jump.targetBlock.hashCode());
                    String targetName = String.valueOf(jump.targetBlock.hashCode());
                    builder.append("  ")
                        .append("jmp ").append(targetName).append("\n");
                }
                case IfNode ifNode -> {
                    System.out.println("append if: " + ifNode.hashCode());
                    String elseBlock = graph.successors(ifNode).stream()
                            .map(b -> String.valueOf(b.hashCode()))
                            .collect(Collectors.joining(", ")).split(", ")[1];
                    String thenBlock = graph.successors(ifNode).stream()
                            .map(b -> String.valueOf(b.hashCode()))
                            .collect(Collectors.joining(", ")).split(", ")[0];
                    //String thenBlock = String.valueOf(ifNode.block().hashCode());
                    //elseBlock = String.valueOf(ifNode.elseBlock().hashCode());
                    Register condition = registers.get(ifNode);
                    builder.append("  ")
                            .append("cmp ").append(condition).append(", 1\n")
                            .append("  ")
                            .append("je ").append(thenBlock).append("\n")
                            .append("jmp").append(elseBlock).append("\n");
                }
                case BitwiseNotNode bitwiseNotNode -> {
                    Register target = registers.get(bitwiseNotNode);
                    Register value = registers.get(predecessorSkipProj(bitwiseNotNode, 0));
                    if (target instanceof x86Registers.OverflowRegisters){
                        builder.append("  ")
                                .append("mov ")
                                .append(x86Registers.RealRegisters.R15D).append(", ")
                                .append(value).append("\n")
                                .append("NEG ").append(x86Registers.RealRegisters.R15D).append("\n")
                                .append("mov ").append(target).append(", ").append(x86Registers.RealRegisters.R15D).append("\n");
                    } else {
                        builder.append("  ")
                            .append("mov ").append(target).append(", ").append(value).append("\n")
                                .append("NEG ").append(target).append("\n");
                    }
                }
                case LogicalNotNode logicalNotNode -> {
                    Register target = registers.get(logicalNotNode);
                    Register value = registers.get(predecessorSkipProj(logicalNotNode, 0));
                    builder.append("  ").append("mov ").append(target).append(", ")
                        .append(value).append("\n")
                            .append("NOT ").append(target).append("\n");
                }
                case Phi phiNode ->{
                    int predecessorIndex = -1;
                    //System.out.println("append phi: " + node.predecessors());
                    List<? extends Node> predecessors = node.block().predecessors();
                    for (int i = 0; i < predecessors.size(); i++) {
                        //System.out.println(i+"  " +predecessors.get(i)+ "  " +predecessors.get(i).block() +" " + current.block());
                        if (predecessors.get(i).block().equals(current.block())) {
                            predecessorIndex = i;
                            break;
                        }
                    }
                    if (predecessorIndex == -1) throw new IllegalStateException("Predecessor not found");

                    Node predecessor = node.predecessor(predecessorIndex);

                    Register target = registers.get(phiNode);
                    Register firstPredecessorRegister = registers.get(predecessor);
                    builder.append("  ")
                        .append("mov ").append(target).append(", ").append(firstPredecessorRegister).append("\n");
                }
                case Block b -> {
                    current = node;
                    System.out.println("append block: " + b.hashCode());
                    builder.append("block").append(b.hashCode()).append(":\n");
                }
                case ProjNode _, StartNode _ -> {
                    // do nothing, skip line break
                }
                default -> builder.append("errror : not implemented node type: ")
                    .append(node.getClass().getSimpleName())
                    .append("\n");
            }
            current = node;
        }
        builder.append("\n");
    }

    private void shiftNode(StringBuilder builder, Map<Node, Register> registers, BinaryOperationNode shiftNode, String opcode) {
        Register target = registers.get(shiftNode);
        Register left = registers.get(predecessorSkipProj(shiftNode, BinaryOperationNode.LEFT));
        int right = ((ConstIntNode)predecessorSkipProj(shiftNode, BinaryOperationNode.RIGHT)).value();
        if (left instanceof x86Registers.OverflowRegisters){
            builder.append("  ")
                    .append("mov ")
                    .append(x86Registers.RealRegisters.R15D).append(", ")
                    .append(left).append("\n");
            builder.append(opcode).append(" ").append(x86Registers.RealRegisters.R15D).append(", ").append(right).append("\n")
                    .append("mov ").append(target).append(", ").append(x86Registers.RealRegisters.R15D).append("\n");
        } else {
            builder.append("  ")
                    .append(opcode).append(" ").append(left).append(", ").append(right).append("\n")
                    .append("mov ").append(target).append(", ").append(left).append("\n");
        }
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
