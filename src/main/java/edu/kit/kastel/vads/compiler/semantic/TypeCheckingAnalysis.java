package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

import java.util.List;
//TODO: Implement type checking logic for the various AST nodes.
public class TypeCheckingAnalysis implements NoOpVisitor<List<ReturnTree>> {
    @Override
    public Unit visit(FunctionTree functionTree, List<ReturnTree> data) {
        // Check function parameters and return type
        for (ReturnTree returnTree : data) {
            System.out.println(returnTree);
            System.out.println(returnTree.expression());
            System.out.println(returnTree.expression().type());
            System.out.println(functionTree.returnType());
            if (!(returnTree.expression().type()).equals(functionTree.returnType().type()) ){
                throw new SemanticException("Function " + functionTree.name() + " does not return the expected type: " +
                        functionTree.returnType().type() + ", but got " + returnTree.expression().type());
            }
        }
        return NoOpVisitor.super.visit(functionTree, data);
    }

    @Override
    public Unit visit(ReturnTree returnTree, List<ReturnTree> data) {
        // Check return statements
        data.add(returnTree);
        return NoOpVisitor.super.visit(returnTree, data);
    }
    @Override
    public Unit visit(DeclarationTree declarationTree, List<ReturnTree> data) {
        // Check variable declarations

        if (declarationTree.initializer() != null) {
            if (!declarationTree.type().equals(declarationTree.initializer().type())) {
                throw new SemanticException("Type mismatch in variable declaration: " + declarationTree.name()+" "+ declarationTree.initializer()+
                        " expected " + declarationTree.type() + " but got " + declarationTree.initializer().type());
            }
        }
        return NoOpVisitor.super.visit(declarationTree, data);
    }

    @Override
    public Unit visit(BinaryOperationTree binaryOperationTree, List<ReturnTree> data) {
        // Check binary operations
        Type lhsType = binaryOperationTree.lhs().type();
        Type rhsType = binaryOperationTree.rhs().type();
        Type inputType = binaryOperationTree.operatorType().inputType();
        if (inputType!=null) {
            if (!lhsType.equals(inputType) || !rhsType.equals(inputType)) {
                throw new SemanticException("Type mismatch in binary operation: " + binaryOperationTree);
            }
        }
        return NoOpVisitor.super.visit(binaryOperationTree, data);
    }


    @Override
    public Unit visit(AssignmentTree assignmentTree, List<ReturnTree> data) {
        // Check assignments
        Type lValueType = ((LValueIdentTree)assignmentTree.lValue()).name().references.type();
        Operator.OperatorType opType = assignmentTree.operator().type();
        Type expressionType = assignmentTree.expression().type();
        if (!lValueType.equals(expressionType)) {
            throw new SemanticException("Type mismatch in assignment: " + assignmentTree);
        }
        if (opType != Operator.OperatorType.ASSIGN) {
            if(opType.inputType().equals(lValueType)) {
                throw new SemanticException("Type mismatch for operator"+ opType +" and value " + lValueType);
            }
        }
        return NoOpVisitor.super.visit(assignmentTree, data);
    }
    // Additional visit methods for other AST nodes can be added here.
}
