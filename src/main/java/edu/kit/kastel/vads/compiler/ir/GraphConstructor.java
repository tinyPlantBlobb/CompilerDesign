package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class GraphConstructor {

    private final Optimizer optimizer;
    private final IrGraph graph;
    private final Map<Name, Map<Block, Node>> currentDef = new HashMap<>();
    private final Map<Block, Map<Name, Phi>> incompletePhis = new HashMap<>();
    private final Map<Block, Node> currentSideEffect = new HashMap<>();
    private final Map<Block, Phi> incompleteSideEffectPhis = new HashMap<>();
    private final Set<Block> sealedBlocks = new HashSet<>();
    private Block currentBlock;

    public GraphConstructor(Optimizer optimizer, String name) {
        this.optimizer = optimizer;
        this.graph = new IrGraph(name);
        this.currentBlock = this.graph.startBlock();
        // the start block never gets any more predecessors
        sealBlock(this.currentBlock);
    }

    public Node newStart() {
        assert currentBlock() == this.graph.startBlock() : "start must be in start block";
        return new StartNode(currentBlock());
    }

    public Node newAdd(Node left, Node right) {
        return this.optimizer.transform(new AddNode(currentBlock(), left, right));
    }
    public Node newSub(Node left, Node right) {
        return this.optimizer.transform(new SubNode(currentBlock(), left, right));
    }

    public Node newMul(Node left, Node right) {
        return this.optimizer.transform(new MulNode(currentBlock(), left, right));
    }

    public Node newDiv(Node left, Node right) {
        return this.optimizer.transform(new DivNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newMod(Node left, Node right) {
        return this.optimizer.transform(new ModNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newReturn(Node result) {
        return new ReturnNode(currentBlock(), readCurrentSideEffect(), result);
    }

    public Node newConstInt(int value) {
        // always move const into start block, this allows better deduplication
        // and resultingly in better value numbering
        return this.optimizer.transform(new ConstIntNode(this.graph.startBlock(), value));
    }

    public Node newSideEffectProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.SIDE_EFFECT);
    }

    public Node newResultProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.RESULT);
    }
    public Node newConstBool(boolean value) {
        // always move const into start block, this allows better deduplication
        // and resultingly in better value numbering
        return this.optimizer.transform(new ConstBoolNode(this.graph.startBlock(), value));
    }
    public Node newAnd(Node left, Node right) {
        return this.optimizer.transform(new LogicalAndNode(currentBlock(), left, right));
    }
    public Node newGreaterThanOrEqual(Node left, Node right) {
        return this.optimizer.transform(new LessThanOrEqualNode(currentBlock(),right,left));
    }
    public Node newLessThanOrEqual(Node left, Node right) {
        return this.optimizer.transform(new LessThanOrEqualNode(currentBlock(), left, right));
    }

    public Node newLessThan(Node left, Node right) {
        return this.optimizer.transform(new LessThanNode(currentBlock(), left, right));
    }
    public Node newEq(Node left, Node right, int size) {
        return this.optimizer.transform(new EqualNode(currentBlock(), left, right, size));
    }
    public Node newNotEqual(Node left, Node right) {
        return this.optimizer.transform(new NotEqualNode(currentBlock(), left, right));
    }
    public Node newJump(Block target) {
        return this.optimizer.transform(new JumpNode(currentBlock(), target));
    }
    public Node newOr(Node left, Node right) {
        return this.optimizer.transform(new LogicalOrNode(currentBlock(), left, right));
    }
    public Node newXor(Node left, Node right) {
        return this.optimizer.transform(new XorNode(currentBlock(), left, right));
    }
    public Node newLeftShift(Node left, Node right) {
        return this.optimizer.transform(new ArithmeticShiftLeftNode(currentBlock(), left, right));
    }
public Node newRightShift(Node left, Node right) {
        return this.optimizer.transform(new ArithmeticShiftRightNode(currentBlock(), left, right));
    }
    public Node newLogicalAnd(Node lhs, Node rhs) {
        return this.optimizer.transform(new LogicalAndNode(currentBlock(), lhs, rhs));
    }

    public Node newLogicalOr(Node lhs, Node rhs) {
        return this.optimizer.transform(new LogicalOrNode(currentBlock(), lhs, rhs));
    }

    public Node newBitwiseNot(Node operand) {
        return this.optimizer.transform(new BitwiseNotNode(currentBlock(), operand));
    }
    public Block currentBlock() {
        return this.currentBlock;
    }

    public Phi newPhi() {
        // don't transform phi directly, it is not ready yet
        return new Phi(currentBlock());
    }

    public IrGraph graph() {
        return this.graph;
    }

    void writeVariable(Name variable, Block block, Node value) {
        this.currentDef.computeIfAbsent(variable, _ -> new HashMap<>()).put(block, value);
    }

    Node readVariable(Name variable, Block block) {
        Node node = this.currentDef.getOrDefault(variable, Map.of()).get(block);
        if (node != null) {
            return node;
        }
        return readVariableRecursive(variable, block);
    }


    private Node readVariableRecursive(Name variable, Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = new Phi(block);
            this.incompletePhis.computeIfAbsent(block, _ -> new HashMap<>()).put(variable, (Phi) val);
        } else if (block.predecessors().size() == 1) {
            val = readVariable(variable, block.predecessors().getFirst().block());
        } else {
            val = new Phi(block);
            writeVariable(variable, block, val);
            val = addPhiOperands(variable, (Phi) val);
        }
        writeVariable(variable, block, val);
        return val;
    }

    Node addPhiOperands(Name variable, Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readVariable(variable, pred.block()));
        }
        return tryRemoveTrivialPhi(phi);
    }

    Node tryRemoveTrivialPhi(Phi phi) {
        Set<Node> predecessorsOfPhi = new HashSet<>(phi.predecessors());
        predecessorsOfPhi.remove(phi);
        if (predecessorsOfPhi.isEmpty()) {
            return new NoDefNode(phi.block()); // no predecessors, so this phi is not needed
        } else if (predecessorsOfPhi.size() == 1) {
            Node replacement = predecessorsOfPhi.iterator().next();
            for (Node successor : graph.successors(phi)) {
                for (int i = 0; i < successor.predecessors().size(); i++) {
                    successor.setPredecessor(i, replacement);
                    if (successor instanceof Phi && sealedBlocks.contains(successor.block())) {
                        tryRemoveTrivialPhi((Phi) successor); // recursively remove trivial phis in successors
                    }
                }
            }
            return replacement;
        }


        return phi;
    }

    void sealBlock(Block block) {
        for (Map.Entry<Name, Phi> entry : this.incompletePhis.getOrDefault(block, Map.of()).entrySet()) {
            addPhiOperands(entry.getKey(), entry.getValue());
        }
        Phi sideEffectPhi = this.incompleteSideEffectPhis.get(block);
        if (sideEffectPhi != null) {
            addPhiOperands(sideEffectPhi);
        }
        this.sealedBlocks.add(block);
    }

    public void writeCurrentSideEffect(Node node) {
        writeSideEffect(currentBlock(), node);
    }

    private void writeSideEffect(Block block, Node node) {
        this.currentSideEffect.put(block, node);
    }

    public Node readCurrentSideEffect() {
        return readSideEffect(currentBlock());
    }

    private Node readSideEffect(Block block) {
        Node node = this.currentSideEffect.get(block);
        if (node != null) {
            return node;
        }
        return readSideEffectRecursive(block);
    }

    private Node readSideEffectRecursive(Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = new Phi(block);
            Phi old = this.incompleteSideEffectPhis.put(block, (Phi) val);
            assert old == null : "double readSideEffectRecursive for " + block;
        } else if (block.predecessors().size() == 1) {
            val = readSideEffect(block.predecessors().getFirst().block());
        } else {
            val = new Phi(block);
            writeSideEffect(block, val);
            val = addPhiOperands((Phi) val);
        }
        writeSideEffect(block, val);
        return val;
    }

    Node addPhiOperands(Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readSideEffect(pred.block()));
        }
        return tryRemoveTrivialPhi(phi);
    }


    public Node newLogicalNot(Node operand) {
        return this.optimizer.transform(new LogicalNotNode(currentBlock(), operand));
    }

    public Node newIf(Node condition) {
        return this.optimizer.transform(new IfNode(currentBlock(), condition));
    }
    public ProjNode newIfTrueProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.IF_TRUE);
    }
    public ProjNode newIfFalseProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.IF_FALSE);
    }

    public Block newBlock(String content) {
        return new Block(this.graph, content);
    }
    public void setCurrentBlock(Block block) {
        assert block != null : "block must not be null";
        assert !this.sealedBlocks.contains(block) : "cannot set current block to a sealed block";
        this.currentBlock = block;
    }
}
