package edu.kit.kastel.vads.compiler.ir.optimize;

import edu.kit.kastel.vads.compiler.ir.node.*;


import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;


public class ConstantFolding implements Optimizer {
    @Override
    public Node transform(Node node) {
        //System.out.println(node.getClass().getSimpleName() + " in constant folding");
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
                            return new DivNode(node.block(), left, right, predecessorSkipProj(binaryOperationNode, DivNode.SIDE_EFFECT));
                        } else if (leftValue== Integer.MIN_VALUE && rightValue==-1) {
                            return node;
                        } else {
                            result =  leftValue / rightValue;
                        }
                    }
                    case ModNode _ -> {
                        if (rightValue == 0) {
                            return new ModNode(node.block(), left, right, predecessorSkipProj(binaryOperationNode, ModNode.SIDE_EFFECT));
                        } else if (leftValue== Integer.MIN_VALUE&&rightValue==-1) {
                            return node;
                        } else {
                            result = leftValue % rightValue;
                        }
                    }
//                    case EqualNode _ -> {
//                        if (leftValue == rightValue) {
//                            return new ConstBoolNode(node.block(), true);
//                        } else {
//                            return new ConstBoolNode(node.block(), false);
//                        }
//                    }
//                    case NotEqualNode _ -> {
//                        if (leftValue != rightValue) {
//                            return new ConstBoolNode(node.block(), true);
//                        } else {
//                            return new ConstBoolNode(node.block(), false);
//                        }
//                    }
//                    case LessThanNode _ -> {
//                        if (leftValue < rightValue) {
//                            return new ConstBoolNode(node.block(), true);
//                        } else {
//                            return new ConstBoolNode(node.block(), false);
//                        }
//                    }
//                    case LessThanOrEqualNode _ -> {
//                        if (leftValue <= rightValue) {
//                            return new ConstBoolNode(node.block(), true);
//                        } else {
//                            return new ConstBoolNode(node.block(), false);
//                        }
//                    }
                    default -> {
                        return node;
                    }
                }
                return new ConstIntNode(node.block(), result);
            } else if (left instanceof ConstBoolNode && right instanceof ConstBoolNode) {
                boolean leftValue = ((ConstBoolNode) left).value();
                boolean rightValue = ((ConstBoolNode) right).value();
                boolean result;
                switch (binaryOperationNode) {
                    case LogicalAndNode _ -> result = leftValue && rightValue;
                    case LogicalOrNode _ -> result = leftValue || rightValue;
                    case EqualNode _ -> result = leftValue == rightValue;
                    case NotEqualNode _ -> result = leftValue != rightValue;
                    default -> {
                        return node;
                    }
                }
                return new ConstBoolNode(node.block(), result);
            }
        }
        return node;
    }
}
