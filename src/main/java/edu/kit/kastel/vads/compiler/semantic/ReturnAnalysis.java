package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.HashSet;
import java.util.Set;

/// Checks that functions return.
/// Currently only works for straight-line code.
class ReturnAnalysis implements NoOpVisitor<ReturnAnalysis.ReturnState> {

    static class ReturnState {
        Set<StatementTree> retrunsStatements = new HashSet<>();
    }

    @Override
    public Unit visit(ReturnTree returnTree, ReturnState data) {
        data.retrunsStatements.add(returnTree);
        return NoOpVisitor.super.visit(returnTree, data);
    }

    @Override
    public Unit visit(BlockTree blockTree, ReturnState data) {
        if(data.retrunsStatements.stream().anyMatch(blockTree.statements()::contains)) {
            data.retrunsStatements.add(blockTree);
        }
        return NoOpVisitor.super.visit(blockTree, data);
    }

    @Override
    public Unit visit(IfTree ifTree, ReturnState data) {
        if (data.retrunsStatements.contains(ifTree.thenTree())&& (ifTree.elseTree() != null && data.retrunsStatements.contains(ifTree.elseTree()))) {
            data.retrunsStatements.add(ifTree);
        }
        return NoOpVisitor.super.visit(ifTree, data);
    }

    @Override
    public Unit visit(FunctionTree functionTree, ReturnState data) {
        if (!data.retrunsStatements.contains(functionTree.body())) {
            throw new SemanticException("function " + functionTree.name() + " does not return");
        }
        data.retrunsStatements.clear();
        return NoOpVisitor.super.visit(functionTree, data);
    }
}
