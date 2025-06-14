package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.lexer.Keyword;
import edu.kit.kastel.vads.compiler.lexer.KeywordType;
import edu.kit.kastel.vads.compiler.parser.ast.*;

import java.util.List;

/// This is a utility class to help with debugging the parser.
public class Printer {

    private final Tree ast;
    private final StringBuilder builder = new StringBuilder();
    private boolean requiresIndent;
    private int indentDepth;

    public Printer(Tree ast) {
        this.ast = ast;
    }

    public static String print(Tree ast) {
        Printer printer = new Printer(ast);
        printer.printRoot();
        return printer.builder.toString();
    }

    private void printRoot() {
        printTree(this.ast);
    }

    private void printTree(Tree tree) {
        switch (tree) {
            case BlockTree(List<StatementTree> statements, _) -> {
                print("{");
                lineBreak();
                this.indentDepth++;
                for (StatementTree statement : statements) {
                    printTree(statement);
                }
                this.indentDepth--;
                print("}");
            }
            case FunctionTree(var returnType, var name, var body) -> {
                printTree(returnType);
                space();
                printTree(name);
                print("()");
                space();
                printTree(body);
            }
            case NameTree(var name, _) -> print(name.asString());
            case ProgramTree(var topLevelTrees) -> {
                for (FunctionTree function : topLevelTrees) {
                    printTree(function);
                    lineBreak();
                }
            }
            case TypeTree(var type, _) -> print(type.asString());
            case BinaryOperationTree(var lhs, var rhs, var op) -> {
                print("(");
                printTree(lhs);
                print(")");
                space();
                this.builder.append(op);
                space();
                print("(");
                printTree(rhs);
                print(")");
            }
            case LiteralIntTree(var value, _, _) -> this.builder.append(value);
            case LiteralBoolTree(var value) -> this.builder.append(value.asString());
            case NegateTree(var expression, _) -> {
                print("-(");
                printTree(expression);
                print(")");
            }
            case AssignmentTree(var lValue, var op, var expression) -> {
                printTree(lValue);
                space();
                this.builder.append(op);
                space();
                printTree(expression);
                semicolon();
            }
            case DeclarationTree(var type, var name, var initializer) -> {
                printTree(type);
                space();
                printTree(name);
                if (initializer != null) {
                    print(" = ");
                    printTree(initializer);
                }
                semicolon();
            }
            case ReturnTree(var expr, _) -> {
                print("return ");
                printTree(expr);
                semicolon();
            }
            case LValueIdentTree(var name) -> printTree(name);
            case IdentExpressionTree(var name) -> printTree(name);
            case BreakTree breakTree -> {
                print("break");
                semicolon();
            }
            case ContinueTree continueTree -> {
                print("continue");
                semicolon();
            }
            case ForTree forTree -> {
                print("for (");
                if (forTree.initialisation() != null) {
                    printTree(forTree.initialisation());
                }
                print("; ");
                if (forTree.condition() != null) {
                    printTree(forTree.condition());
                }
                print("; ");
                if (forTree.step() != null) {
                    printTree(forTree.step());
                }
                print(") ");
                printTree(forTree.loopBody());
            }
            case IfTree ifTree -> {
                print("if (");
                printTree(ifTree.condition());
                print(") ");
                printTree(ifTree.thenTree());
                if (ifTree.elseTree() != null) {
                    print(" else ");
                    printTree(ifTree.elseTree());
            }
            }
            case WhileTree whileTree -> {
                print("while (");
                printTree(whileTree.condition());
                print(") ");
                printTree(whileTree.loopBody());
            }
            case UnaryOperationTree unaryOperationTree -> {
                print(unaryOperationTree.operand().asString());
                print("(");
                printTree(unaryOperationTree.expression());
                print(")");
            }
            case TernaryOperationTree ternaryOperationTree -> {
                print("(");
                printTree(ternaryOperationTree.condition());
                print(") ? ");
                printTree(ternaryOperationTree.thenExpression());
                print(" : ");
                printTree(ternaryOperationTree.elseExpression());
            }
            default -> {
                throw new IllegalArgumentException("Unknown tree type: " + tree.getClass().getSimpleName());
            }

        }
    }

    private void print(String str) {
        if (this.requiresIndent) {
            this.requiresIndent = false;
            this.builder.append(" ".repeat(4 * this.indentDepth));
        }
        this.builder.append(str);
    }

    private void lineBreak() {
        this.builder.append("\n");
        this.requiresIndent = true;
    }

    private void semicolon() {
        this.builder.append(";");
        lineBreak();
    }

    private void space() {
        this.builder.append(" ");
    }

}
