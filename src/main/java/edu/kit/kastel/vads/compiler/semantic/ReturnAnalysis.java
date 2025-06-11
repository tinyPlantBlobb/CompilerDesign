package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

/// Checks that functions return.
/// Currently only works for straight-line code.
class ReturnAnalysis implements NoOpVisitor<ReturnAnalysis.ReturnState> {

    static class ReturnState {
        boolean returns = false;
    }

    @Override
    public Unit visit(ReturnTree returnTree, ReturnState data) {
        data.returns = true;
        return NoOpVisitor.super.visit(returnTree, data);
    }

    @Override
    public Unit visit(IfTree ifTree, ReturnState data) {
        return null;
    }

    @Override
    public Unit visit(WhileTree whileTree, ReturnState data) {
        return null;
    }

    @Override
    public Unit visit(ForTree forTree, ReturnState data) {
        return null;
    }

    @Override
    public Unit visit(BreakTree breakTree, ReturnState data) {
        return null;
    }

    @Override
    public Unit visit(ContinueTree continueTree, ReturnState data) {
        return null;
    }

    @Override
    public Unit visit(FunctionTree functionTree, ReturnState data) {
        if (!data.returns) {
            throw new SemanticException("function " + functionTree.name() + " does not return");
        }
        data.returns = false;
        return NoOpVisitor.super.visit(functionTree, data);
    }
}
