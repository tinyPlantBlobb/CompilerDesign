package edu.kit.kastel.vads.compiler.ir.optimize;

import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;


import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;


public class ConstantFolding implements Optimizer {
    @Override
    public Node transform(Node node) {
        if (node instanceof BinaryOperationNode binaryOperationNode) {
            Node left = predecessorSkipProj(binaryOperationNode, BinaryOperationNode.LEFT);
            Node right = predecessorSkipProj(binaryOperationNode,BinaryOperationNode.RIGHT);

            if (left instanceof ConstIntNode && right instanceof ConstIntNode) {
                int leftValue = ((ConstIntNode) left).value();
                int rightValue = ((ConstIntNode) right).value();
                int result = 0;
                switch (binaryOperationNode) {
                    case AddNode _ -> result = leftValue + rightValue;
                    case SubNode _ -> result =  leftValue - rightValue;
                    case MulNode _ -> result =  leftValue * rightValue;
                    case DivNode _ -> {
                        if (((ConstIntNode) right).value() == 0) {
                            return node;
                        } else {
                            result =  leftValue / rightValue;
                        }
                    }
                    case ModNode _ -> {
                        if (rightValue == 0) {
                            return node;
                        } else if (leftValue== Integer.MIN_VALUE&&rightValue==-1) {
                            return node;
                        } else {
                            result = leftValue % rightValue;
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + binaryOperationNode);
                }
                return new ConstIntNode(node.block(), result);
            }
        }
        return node;
    }
}
