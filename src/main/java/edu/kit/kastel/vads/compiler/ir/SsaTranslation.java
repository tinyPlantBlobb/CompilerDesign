package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfo;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfoHelper;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.BinaryOperator;

/// SSA translation as described in
/// [`Simple and Efficient Construction of Static Single Assignment Form`](https://compilers.cs.uni-saarland.de/papers/bbhlmz13cc.pdf).
///
/// This implementation also tracks side effect edges that can be used to avoid reordering of operations that cannot be
/// reordered.
///
/// We recommend to read the paper to better understand the mechanics implemented here.
public class SsaTranslation {
  private final FunctionTree function;
  private final GraphConstructor constructor;

  public SsaTranslation(FunctionTree function, Optimizer optimizer) {
    this.function = function;
    this.constructor = new GraphConstructor(optimizer, function.name().name().asString());
  }

  public IrGraph translate() {
    var visitor = new SsaTranslationVisitor();
    this.function.accept(visitor, this);
    return this.constructor.graph();
  }

  private void writeVariable(Name variable, Block block, Node value) {
    this.constructor.writeVariable(variable, block, value);
  }

  private Node readVariable(Name variable, Block block) {
    return this.constructor.readVariable(variable, block);
  }

  private Block currentBlock() {
    return this.constructor.currentBlock();
  }

  private static class SsaTranslationVisitor implements Visitor<SsaTranslation, Optional<Node>> {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final Optional<Node> NOT_AN_EXPRESSION = Optional.empty();

    private final Deque<DebugInfo> debugStack = new ArrayDeque<>();

    private void pushSpan(Tree tree) {
      this.debugStack.push(DebugInfoHelper.getDebugInfo());
      DebugInfoHelper.setDebugInfo(new DebugInfo.SourceInfo(tree.span()));
    }

    private void popSpan() {
      DebugInfoHelper.setDebugInfo(this.debugStack.pop());
    }

    @Override
    public Optional<Node> visit(AssignmentTree assignmentTree, SsaTranslation data) {
      pushSpan(assignmentTree);
      BinaryOperator<Node> desugar = switch (assignmentTree.operator().type()) {
        case ASSIGN_MINUS -> data.constructor::newSub;
        case ASSIGN_PLUS -> data.constructor::newAdd;
        case ASSIGN_MUL -> data.constructor::newMul;
        case ASSIGN_DIV -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
        case ASSIGN_MOD -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));
        case ASSIGN_BITWISE_AND -> data.constructor::newAnd;
        case ASSIGN_BITWISE_OR -> data.constructor::newOr;
        case ASSIGN_BITWISE_XOR -> data.constructor::newXor;
        case ASSIGN_LEFT_SHIFT -> data.constructor::newLeftShift;
        case ASSIGN_RIGHT_SHIFT -> data.constructor::newRightShift;
        case ASSIGN -> null;
        default ->
          throw new IllegalArgumentException("not an assignment operator " + assignmentTree.operator());
      };

      switch (assignmentTree.lValue()) {
        case LValueIdentTree(var name) -> {
          Node rhs = assignmentTree.expression().accept(this, data).orElseThrow();
          if (desugar != null) {
            rhs = desugar.apply(data.readVariable(name.name(), data.currentBlock()), rhs);
          }
          data.writeVariable(name.name(), data.currentBlock(), rhs);
        }
      }
      popSpan();
      return NOT_AN_EXPRESSION;
    }

    @Override
    public Optional<Node> visit(BinaryOperationTree binaryOperationTree, SsaTranslation data) {
      pushSpan(binaryOperationTree);
      Node lhs = binaryOperationTree.lhs().accept(this, data).orElseThrow();
      Node rhs = binaryOperationTree.rhs().accept(this, data).orElseThrow();
      Node res = switch (binaryOperationTree.operatorType()) {
        case MINUS -> data.constructor.newSub(lhs, rhs);
        case PLUS -> data.constructor.newAdd(lhs, rhs);
        case MUL -> data.constructor.newMul(lhs, rhs);
        case DIV -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
        case MOD -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));

        case LEFT_SHIFT -> data.constructor.newLeftShift(lhs, rhs);
        case RIGHT_SHIFT -> data.constructor.newRightShift(lhs, rhs);

        case LOGICAL_AND -> data.constructor.newLogicalAnd(lhs, rhs);
        case LOGICAL_OR -> data.constructor.newLogicalOr(lhs, rhs);
        case BITWISE_XOR -> data.constructor.newXor(lhs, rhs);
        case BITWISE_AND -> data.constructor.newAnd(lhs, rhs);
        case BITWISE_OR -> data.constructor.newOr(lhs, rhs);
        case LOGICAL_EQUAL -> {
            int size = 4;
            if(binaryOperationTree.lhs().type().equals(BasicType.INT)){
              size = 1;
            }
            yield data.constructor.newEq(lhs, rhs, size);
        }
        case LOGICAL_NOT_EQUAL -> data.constructor.newNotEqual(lhs, rhs);
        case LESS_EQUAL -> data.constructor.newLessThanOrEqual(lhs, rhs);
        case GREATER_EQUAL -> data.constructor.newGreaterThanOrEqual(rhs, lhs);
        case LESS_THAN -> data.constructor.newLessThan(lhs, rhs);
        case GREATER_THAN -> data.constructor.newLessThan(rhs, lhs);

        default ->
          throw new IllegalArgumentException("not a binary expression operator " + binaryOperationTree.operatorType());
      };
      popSpan();
      return Optional.of(res);
    }

    @Override
    public Optional<Node> visit(BlockTree blockTree, SsaTranslation data) {
      pushSpan(blockTree);
      for (StatementTree statement : blockTree.statements()) {
        statement.accept(this, data);
        // skip everything after a return in a block
        if (statement instanceof ReturnTree) {
          break;
        }
      }
      popSpan();
      return NOT_AN_EXPRESSION;
    }

    @Override
    public Optional<Node> visit(DeclarationTree declarationTree, SsaTranslation data) {
      pushSpan(declarationTree);
      if (declarationTree.initializer() != null) {
        System.out.println(declarationTree.initializer().accept(this, data));
        Node rhs = declarationTree.initializer().accept(this, data).orElseThrow();
        data.writeVariable(declarationTree.name().name(), data.currentBlock(), rhs);
      }
      popSpan();
      return NOT_AN_EXPRESSION;
    }

    @Override
    public Optional<Node> visit(FunctionTree functionTree, SsaTranslation data) {
      pushSpan(functionTree);
      Node start = data.constructor.newStart();
      data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(start));
      functionTree.body().accept(this, data);
      popSpan();
      return NOT_AN_EXPRESSION;
    }

    @Override
    public Optional<Node> visit(IdentExpressionTree identExpressionTree, SsaTranslation data) {
      pushSpan(identExpressionTree);
      Node value = data.readVariable(identExpressionTree.name().name(), data.currentBlock());
      popSpan();
      return Optional.of(value);
    }

    @Override
    public Optional<Node> visit(LiteralIntTree literalTree, SsaTranslation data) {
      pushSpan(literalTree);
      Node node = data.constructor.newConstInt((int) literalTree.parseValue().orElseThrow());
      popSpan();
      return Optional.of(node);
    }

    @Override
    public Optional<Node> visit(LiteralBoolTree literalTree, SsaTranslation data) {
      pushSpan(literalTree);
      Node node = data.constructor.newConstBool(literalTree.parseValue());
      return Optional.of(node);
    }


    @Override
    public Optional<Node> visit(LValueIdentTree lValueIdentTree, SsaTranslation data) {
      return NOT_AN_EXPRESSION;
    }

    @Override
    public Optional<Node> visit(NameTree nameTree, SsaTranslation data) {
      return NOT_AN_EXPRESSION;
    }

    @Override
    public Optional<Node> visit(NegateTree negateTree, SsaTranslation data) {
      pushSpan(negateTree);
      Node node = negateTree.expression().accept(this, data).orElseThrow();
      Node res = data.constructor.newSub(data.constructor.newConstInt(0), node);
      popSpan();
      return Optional.of(res);
    }

    @Override
    public Optional<Node> visit(ProgramTree programTree, SsaTranslation data) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Node> visit(ReturnTree returnTree, SsaTranslation data) {
      pushSpan(returnTree);
      Node node = returnTree.expression().accept(this, data).orElseThrow();
      Node ret = data.constructor.newReturn(node);
      data.constructor.graph().endBlock().addPredecessor(ret);
      popSpan();
      return NOT_AN_EXPRESSION;
    }

    @Override
    public Optional<Node> visit(TypeTree typeTree, SsaTranslation data) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Node> visit(IfTree ifTree, SsaTranslation data) {
        pushSpan(ifTree);
        Node condition = ifTree.condition().accept(this, data).orElseThrow();
        IfProjections ifProjection = ProjectIfNode(data, condition);
        data.constructor.sealBlock(data.constructor.currentBlock());

        Node projNode = processBranch(ifTree.thenTree(), ifProjection.trueProj, "if_then", data);
        Node elseProj = processBranch(ifTree.elseTree(), ifProjection.falseProj, "if_else", data);
        Block endBlock = data.constructor.newBlock("if_end");
        data.constructor.setCurrentBlock(endBlock);
        endBlock.addPredecessor(projNode);
        endBlock.addPredecessor(elseProj);
        data.constructor.sealBlock(endBlock);
        popSpan();
        return NOT_AN_EXPRESSION;
    }
    private Node processBranch(StatementTree branch, ProjNode projNode, String label, SsaTranslation data) {
      Block branchBlock = data.constructor.newBlock(label);
        branchBlock.addPredecessor(projNode);
        data.constructor.sealBlock(branchBlock);
        if (branch != null) {
            // if there is no else branch, we just return the projNode
          branch.accept(this, data);

        }

        data.constructor.sealBlock(data.constructor.currentBlock());
        return data.constructor.newJump(data.currentBlock());
    }

    @Override
    public Optional<Node> visit(WhileTree whileTree, SsaTranslation data) {
        pushSpan(whileTree);
      data.constructor.sealBlock(data.constructor.currentBlock());
      Node exitJump = data.constructor.newJump(data.constructor.currentBlock());

      Block whileBlock = data.constructor.newBlock("while");
      whileBlock.addPredecessor(exitJump);
        data.constructor.setCurrentBlock(whileBlock);
        data.constructor.pushLoopBlock(whileBlock);
        var condition = whileTree.condition().accept(this, data).orElseThrow();

        IfProjections ifProjection = ProjectIfNode(data, condition);

        Block followBlock = data.constructor.newBlock("while_follow");
        followBlock.addPredecessor(ifProjection.falseProj);
        data.constructor.pushLoopFollow(followBlock);

        Block bodyBlock = data.constructor.newBlock("while_body");
        bodyBlock.addPredecessor(ifProjection.trueProj);
        data.constructor.sealBlock(bodyBlock);
        data.constructor.setCurrentBlock(bodyBlock);
        whileTree.loopBody().accept(this, data).orElseThrow();
        Node conditionJump = data.constructor.newJump(data.constructor.currentBlock());
        data.constructor.sealBlock(data.constructor.currentBlock());
        whileBlock.addPredecessor(conditionJump);

        data.constructor.popLoopBlock(whileBlock);
        data.constructor.sealBlock(whileBlock);
        data.constructor.popLoopFollow(followBlock);
        data.constructor.sealBlock(followBlock);
        data.constructor.setCurrentBlock(followBlock);
        popSpan();
      return NOT_AN_EXPRESSION;
    }

    @Override
    public Optional<Node> visit(ForTree forTree, SsaTranslation data) {
    pushSpan(forTree);
    // For loops are desugared to a while loop, so we can just use the whileTree visitor
    //Node init = forTree.initializer().accept(this, data).orElseThrow();
    forTree.initialisation().accept(this, data).orElseThrow();
    data.constructor.sealBlock(data.constructor.currentBlock());
    Node entryJump = data.constructor.newJump(data.constructor.currentBlock());

    Block forBlock = data.constructor.newBlock("for");
      forBlock.addPredecessor(entryJump);
      Block followBlock = data.constructor.newBlock("for_follow");
      Block bodyBlock = data.constructor.newBlock("for_body");
      Block stepBlock = data.constructor.newBlock("for_step");

      data.constructor.pushLoopBlock(stepBlock);
      data.constructor.popLoopFollow(followBlock);
        data.constructor.setCurrentBlock(forBlock);

      var condition = forTree.condition().accept(this, data).orElseThrow();
      IfProjections ifProjection = ProjectIfNode(data, condition);
      bodyBlock.addPredecessor(ifProjection.trueProj);
      followBlock.addPredecessor(ifProjection.falseProj);
      data.constructor.setCurrentBlock(stepBlock);

      if (forTree.step() != null) {
        forTree.step().accept(this, data).orElseThrow();
      }
      Node stepExitJump = data.constructor.newJump(data.constructor.currentBlock());
      forBlock.addPredecessor(stepExitJump);

      data.constructor.setCurrentBlock(bodyBlock);
      forTree.loopBody().accept(this, data).orElseThrow();
      Node normalLoopExit = data.constructor.newJump(data.constructor.currentBlock());
      stepExitJump.addPredecessor(normalLoopExit);
      data.constructor.sealBlock(forBlock);
      data.constructor.sealBlock(followBlock);
      data.constructor.sealBlock(stepBlock);
      data.constructor.sealBlock(bodyBlock);

      data.constructor.popLoopFollow(followBlock);
      data.constructor.popLoopBlock(stepBlock);
      data.constructor.setCurrentBlock(followBlock);

    popSpan();
      return NOT_AN_EXPRESSION;
    }

    @Override
    public Optional<Node> visit(BreakTree breakTree, SsaTranslation data) {
        pushSpan(breakTree);
        Node jump = data.constructor.newJump(data.constructor.currentBlock());
      data.constructor.getLoopFollow().addPredecessor(jump);
      data.constructor.sealBlock(data.constructor.currentBlock());
      data.constructor.setCurrentBlock(data.constructor.newBlock("following_break"));
      popSpan();
      return NOT_AN_EXPRESSION;
    }

    @Override
    public Optional<Node> visit(ContinueTree continueTree, SsaTranslation data){
      pushSpan(continueTree);
    Node jump = data.constructor.newJump(data.constructor.currentBlock());
        data.constructor.getLoopBlock().addPredecessor(jump);
        data.constructor.sealBlock(data.constructor.currentBlock());
        data.constructor.setCurrentBlock(data.constructor.newBlock("following_continue"));
    popSpan();
        return NOT_AN_EXPRESSION;

    }


    @Override
    public Optional<Node> visit(UnaryOperationTree unaryOperationTree, SsaTranslation data) {
      pushSpan(unaryOperationTree);
      Node operand = unaryOperationTree.expression().accept(this, data).orElseThrow();
      Node res = switch (unaryOperationTree.operand().type()) {
        case MINUS -> data.constructor.newSub(data.constructor.newConstInt(0), operand);
        case BITWISE_NOT -> data.constructor.newBitwiseNot(operand);
        case LOGICAL_NOT -> data.constructor.newLogicalNot(operand);
        default ->
          throw new IllegalArgumentException("not a unary expression operator " + unaryOperationTree.operand().type());
      };
      popSpan();
      return Optional.of(res);
    }

    @Override
    public Optional<Node> visit(TernaryOperationTree ternaryOperationTree, SsaTranslation data) {
      pushSpan(ternaryOperationTree);
      Node condition = ternaryOperationTree.condition().accept(this, data).orElseThrow();
      IfProjections ifProjection = ProjectIfNode(data, condition);
      data.constructor.sealBlock(data.constructor.currentBlock());
      Block trueBlock = data.constructor.newBlock("terniary_then");
      Block falseBlock = data.constructor.newBlock("terniary_else");
      trueBlock.addPredecessor(ifProjection.trueProj);
      falseBlock.addPredecessor(ifProjection.falseProj);
      data.constructor.sealBlock(trueBlock);
      data.constructor.sealBlock(falseBlock);

      data.constructor.setCurrentBlock(trueBlock);
      Node thenBranch = ternaryOperationTree.thenExpression().accept(this, data).orElseThrow();
      Node thenJump = data.constructor.newJump(trueBlock);
      data.constructor.setCurrentBlock(falseBlock);
      Node elseBranch = ternaryOperationTree.elseExpression().accept(this, data).orElseThrow();
      Node elseJump = data.constructor.newJump(falseBlock);
      Block endBlock = data.constructor.newBlock("terniary_end");
      data.constructor.setCurrentBlock(endBlock);
      endBlock.addPredecessor(thenJump);
      endBlock.addPredecessor(elseJump);
      data.constructor.sealBlock(endBlock);
      Phi value = data.constructor.newPhi();
      value.addPredecessor(thenBranch);
      value.addPredecessor(elseBranch);
      data.constructor.setCurrentBlock(endBlock);
      return Optional.of(data.constructor.tryRemoveTrivialPhi(value));
    }

    private IfProjections ProjectIfNode(SsaTranslation data, Node condition) {
      Node ifNode = data.constructor.newIf(condition);
      ProjNode trueProj = data.constructor.newIfTrueProj(ifNode);
      ProjNode falseProj = data.constructor.newIfFalseProj(ifNode);
      return new IfProjections(trueProj, falseProj);
    }
    private record IfProjections(ProjNode trueProj, ProjNode falseProj) {}

    private Node projResultDivMod(SsaTranslation data, Node divMod) {
      // make sure we actually have a div or a mod, as optimizations could
      // have changed it to something else already
      if (!(divMod instanceof DivNode || divMod instanceof ModNode)) {
        return divMod;
      }
      Node projSideEffect = data.constructor.newSideEffectProj(divMod);
      data.constructor.writeCurrentSideEffect(projSideEffect);
      return data.constructor.newResultProj(divMod);
    }
  }

}
