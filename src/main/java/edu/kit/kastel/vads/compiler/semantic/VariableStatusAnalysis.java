package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.*;

import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import org.jspecify.annotations.Nullable;

import java.util.Locale;


/// Checks that variables are
/// - declared before assignment
/// - not declared twice
/// - not initialized twice
/// - assigned before referenced
class VariableStatusAnalysis implements NoOpVisitor<Namespace<VariableStatusAnalysis.VariableStatus>> {

    @Override
    public Unit visit(AssignmentTree assignmentTree, Namespace<VariableStatus> data) {
        switch (assignmentTree.lValue()) {
            case LValueIdentTree(var name) -> {
                VariableStatus status = data.get(name);
                if (assignmentTree.operator().type() == Operator.OperatorType.ASSIGN) {
                    checkDeclared(name, status);
                } else {
                    checkInitialized(name, status);
                }
                if (status != VariableStatus.INITIALIZED) {
                    // only update when needed, reassignment is totally fine
                    updateStatus(data, VariableStatus.INITIALIZED, name);
                }
            }
        }
        return NoOpVisitor.super.visit(assignmentTree, data);
    }

    private static void checkDeclared(NameTree name, @Nullable VariableStatus status) {
        if (status == null) {
            throw new SemanticException("Variable " + name + " must be declared before assignment");
        }
    }

    private static void checkInitialized(NameTree name, @Nullable VariableStatus status) {
        if (status == null || status == VariableStatus.DECLARED) {
            throw new SemanticException("Variable " + name + " must be initialized before use");
        }
    }

    private static void checkUndeclared(NameTree name, @Nullable VariableStatus status) {
        System.out.println("Checking undeclared for: " + name + ", status: " + status);
        if (status != null) {
            throw new SemanticException("Variable " + name + " is already declared");
        }
    }
//
//    private static VariableStatus defineAllUninitialized(Namespace<VariableStatus> data) {
//        // Initialize all variables as DECLARED
//        for (Name name : data.content.keySet()) {
//            if (data.get(name) == null) {
//                data.put(NameTree(name), VariableStatus.DECLARED, (existing, replacement) -> existing);
//            }
//            data.put(NameTree.of(name), VariableStatus.DECLARED, (existing, replacement) -> existing);
//        }
//        return VariableStatus.DECLARED;
//    }

    @Override
    public Unit visit(DeclarationTree declarationTree, Namespace<VariableStatus> data) {
        checkUndeclared(declarationTree.name(), data.get(declarationTree.name()));
        VariableStatus status = declarationTree.initializer() == null
            ? VariableStatus.DECLARED
            : VariableStatus.INITIALIZED;
        updateStatus(data, status, declarationTree.name());
        return NoOpVisitor.super.visit(declarationTree, data);
    }

    private static void updateStatus(Namespace<VariableStatus> data, VariableStatus status, NameTree name) {
        data.put(name, status, (existing, replacement) -> {
            if (existing.ordinal() >= replacement.ordinal()) {
                throw new SemanticException("variable is already " + existing + ". Cannot be " + replacement + " here.");
            }
            return replacement;
        });
    }

    @Override
    public Unit visit(IdentExpressionTree identExpressionTree, Namespace<VariableStatus> data) {
        VariableStatus status = data.get(identExpressionTree.name());
        checkInitialized(identExpressionTree.name(), status);
        return NoOpVisitor.super.visit(identExpressionTree, data);
    }
    @Override
    public Unit visit(LValueIdentTree lValueIdentTree, Namespace<VariableStatus> data) {

        VariableStatus status = data.get(lValueIdentTree.name());
        checkDeclared(lValueIdentTree.name(), status);
        return NoOpVisitor.super.visit(lValueIdentTree, data);
    }

    @Override
    public Unit visit(IfTree ifTree, Namespace<VariableStatus> data) {

        ifTree.condition().accept(this, data);

        if (ifTree.elseTree() != null) {
            // If-else branches can share the same variable status
            ifTree.thenTree().accept(this, data);
            ifTree.elseTree().accept(this, data);
        } else {
            // Only the then branch is executed
            ifTree.thenTree().accept(this, data);
        }
        return NoOpVisitor.super.visit(ifTree, data);

    }
    @Override
    public Unit visit(ReturnTree returnTree, Namespace<VariableStatus> data) {

        if (returnTree.expression() != null) {
            returnTree.expression().accept(this, data);
        }
        return NoOpVisitor.super.visit(returnTree, data);
    }

    @Override
    public Unit visit(WhileTree whileTree, Namespace<VariableStatus> data) {

        whileTree.condition().accept(this, data);
        Namespace<VariableStatus> loopData = enterNamespace(data);
        whileTree.loopBody().accept(this, loopData);
        return NoOpVisitor.super.visit(whileTree, data);
    }

    @Override
    public Unit visit(ForTree forTree, Namespace<VariableStatus> data) {

        forTree.condition().accept(this, data);
        Namespace<VariableStatus> loopData = enterNamespace(data);
        if (forTree.initialisation() != null) {
            forTree.initialisation().accept(this, loopData);
        }
        if (forTree.step() != null) {
            forTree.step().accept(this, loopData);
        }
        forTree.loopBody().accept(this, loopData);
        return NoOpVisitor.super.visit(forTree, data);
    }

    @Override
    public Unit visit(BreakTree breakTree, Namespace<VariableStatus> data) {

        return NoOpVisitor.super.visit(breakTree, data);
    }

    @Override
    public Unit visit(ContinueTree continueTree, Namespace<VariableStatus> data) {
        return NoOpVisitor.super.visit(continueTree, data);
    }

    @Override
    public Unit visit(TernaryOperationTree ternaryOperationTree, Namespace<VariableStatus> data) {

        ternaryOperationTree.condition().accept(this, data);
        ternaryOperationTree.thenExpression().accept(this, data);
        ternaryOperationTree.elseExpression().accept(this, data);
        return NoOpVisitor.super.visit(ternaryOperationTree, data);
    }

    @Override
    public Unit visit(TypeTree typeTree, Namespace<VariableStatus> data) {
        // TypeTree does not have any variable status, so we just return
        return NoOpVisitor.super.visit(typeTree, data);
    }

    private Namespace<VariableStatus> enterNamespace(Namespace<VariableStatus> data) {
        // Create a new namespace for the block
        return new Namespace<>(data.content);
    }

    enum VariableStatus {
        DECLARED,
        INITIALIZED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
