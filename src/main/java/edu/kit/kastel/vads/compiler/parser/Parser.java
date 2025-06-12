package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.lexer.*;
import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final TokenSource tokenSource;
    private final List<KeywordType> types  = List.of(KeywordType.INT, KeywordType.BOOL);
    public Parser(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }

    public ProgramTree parseProgram() {
        ProgramTree programTree = new ProgramTree(List.of(parseFunction()));
        if (this.tokenSource.hasMore()) {
            throw new ParseException("expected end of input but got " + this.tokenSource.peek());
        }
        return programTree;
    }

    private FunctionTree parseFunction() {
        TypeTree returnType = parseType();
        Identifier identifier = this.tokenSource.expectIdentifier();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        BlockTree body = parseBlock();
        return new FunctionTree(
           returnType,
            name(identifier),
            body
        );
    }

    private BlockTree parseBlock() {
        Separator bodyOpen = this.tokenSource.expectSeparator(SeparatorType.BRACE_OPEN);
        List<StatementTree> statements = new ArrayList<>();
        while (!(this.tokenSource.peek() instanceof Separator sep && sep.type() == SeparatorType.BRACE_CLOSE)) {
            statements.add(parseStatement());
        }
        Separator bodyClose = this.tokenSource.expectSeparator(SeparatorType.BRACE_CLOSE);
        return new BlockTree(statements, bodyOpen.span().merge(bodyClose.span()));
    }

    private StatementTree parseStatement() {
        StatementTree statement;
        if (this.tokenSource.peek().isKeyword(KeywordType.INT) || this.tokenSource.peek().isKeyword(KeywordType.BOOL)) {
            statement = parseDeclaration();
        } else if (this.tokenSource.peek().isControlFlow()) {
            statement = parseControlFlow();
        }
        else if (this.tokenSource.peek().isKeyword(KeywordType.RETURN)) {
            statement = parseReturn();
        } else {
            statement = parseSimple();
        }
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        return statement;
    }
    private TypeTree parseType() {

        Keyword type = this.tokenSource.expectAnyKeyword(this.types);

        BasicType basicType = null;
        if (type.isKeyword(KeywordType.INT)) {
            basicType = BasicType.INT;
        } else if (type.isKeyword(KeywordType.BOOL)) {
            basicType = BasicType.BOOL;
        } else {
            throw new ParseException("expected keyword type but got " + this.tokenSource.peek());
        }


        return new TypeTree(basicType, type.span());
    }
    private StatementTree parseDeclaration() {
        TypeTree typetree = parseType();
        Identifier ident = this.tokenSource.expectIdentifier();
        ExpressionTree expr = null;
        if (this.tokenSource.peek().isOperator(OperatorType.ASSIGN)) {
                this.tokenSource.expectOperator(OperatorType.ASSIGN);
                expr = parseExpression();
        }
        NameTree name = name(ident);
        DeclarationTree declarationTree = new DeclarationTree(typetree, name, expr);
        name.addReference(declarationTree);
        return declarationTree;

    }

    private StatementTree parseSimple() {
        LValueTree lValue = parseLValue();
        Operator assignmentOperator = parseAssignmentOperator();
        ExpressionTree expression = parseExpression();
        return new AssignmentTree(lValue, assignmentOperator, expression);
    }

    private Operator parseAssignmentOperator() {
        if (this.tokenSource.peek() instanceof Operator op) {
            return switch (op.type()) {
                case ASSIGN, ASSIGN_DIV, ASSIGN_MINUS, ASSIGN_MOD, ASSIGN_MUL, ASSIGN_PLUS, ASSIGN_BITWISE_AND, ASSIGN_BITWISE_OR, ASSIGN_BITWISE_XOR -> {
                    this.tokenSource.consume();
                    yield op;
                }
                default -> throw new ParseException("expected assignment but got " + op.type());
            };
        }
        throw new ParseException("expected assignment but got " + this.tokenSource.peek());
    }

    private LValueTree parseLValue() {
        if (this.tokenSource.peek().isSeparator(SeparatorType.PAREN_OPEN)) {
            this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
            LValueTree inner = parseLValue();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
            return inner;
        }
        Identifier identifier = this.tokenSource.expectIdentifier();
        return new LValueIdentTree(name(identifier));
    }

    private StatementTree parseReturn() {
        Keyword ret = this.tokenSource.expectKeyword(KeywordType.RETURN);
        ExpressionTree expression = parseExpression();
        return new ReturnTree(expression, ret.span().start());
    }

    private ExpressionTree parseExpression() {
        ExpressionTree lhs = parseTerm();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _)
                && (type == OperatorType.PLUS || type == OperatorType.MINUS)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseTerm(), type);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseTerm() {
        ExpressionTree lhs = parseFactor();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _)
                && (type == OperatorType.MUL || type == OperatorType.DIV || type == OperatorType.MOD)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseFactor(), type);
            } else {
                return lhs;
            }
        }
    }
    private ControlFlowTree parseControlFlow() {
        Token token = this.tokenSource.peek();
        switch (token) {
            case Keyword(var type, Span span) when type == KeywordType.IF -> {
                this.tokenSource.consume();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
                ExpressionTree condition = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                BlockTree thenBlock = parseBlock();
                BlockTree elseBlock = null;
                if (this.tokenSource.peek().isKeyword(KeywordType.ELSE)) {
                    this.tokenSource.expectKeyword(KeywordType.ELSE);
                    elseBlock = parseBlock();
                }
                return  new IfTree(condition, thenBlock, elseBlock, span.start());
            }
            case Keyword(var type, Span span) when type == KeywordType.WHILE -> {
                this.tokenSource.consume();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
                ExpressionTree condition = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                BlockTree body = parseBlock();
                return new WhileTree(condition, body, span.start());
            }
            case Keyword(var type, Span span) when type == KeywordType.FOR -> {
                this.tokenSource.consume();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
                TypeTree typetree = parseType();
                Identifier ident = this.tokenSource.expectIdentifier();
                this.tokenSource.expectOperator(OperatorType.ASSIGN);
                ExpressionTree start = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
                ExpressionTree condition = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
                StatementTree step = parseStatement();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                BlockTree body = parseBlock();
                NameTree name = name(ident);
                DeclarationTree declarationTree = new DeclarationTree(typetree, name, start);
                name.addReference(declarationTree);
                return new ForTree(declarationTree, condition, step, body, span.start());
            }
            case Keyword(var type, Span span) when type == KeywordType.BREAK -> {
                this.tokenSource.consume();
                return new BreakTree(span);
            }
            case Keyword(var type, Span span) when type == KeywordType.CONTINUE -> {
                this.tokenSource.consume();
                return new ContinueTree(span);
            }
            case Keyword(var type, Span span) when type == KeywordType.RETURN -> {
                this.tokenSource.consume();
                ExpressionTree expression = parseExpression();
                return new ReturnTree(expression, span.start());
            }
            default -> throw new ParseException("expected control flow but got " + token);
        }

    }


    private ExpressionTree parseFactor() {
        return switch (this.tokenSource.peek()) {
            case Separator(var type, _) when type == SeparatorType.PAREN_OPEN -> {
                this.tokenSource.consume();
                ExpressionTree expression = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                yield expression;
            }
            case Operator(var type, _) when type == OperatorType.MINUS -> {
                Span span = this.tokenSource.consume().span();
                yield new NegateTree(parseFactor(), span);
            }
            case Identifier ident -> {
                this.tokenSource.consume();
                yield new IdentExpressionTree(name(ident));
            }
            case NumberLiteral(String value, int base, Span span) -> {
                this.tokenSource.consume();
                yield new LiteralIntTree(value, base, span);
            }
            case Keyword(var type, Span span) when type == KeywordType.TRUE||type==KeywordType.FALSE -> {
                this.tokenSource.consume();
                yield new LiteralBoolTree(new Keyword(type==KeywordType.TRUE? KeywordType.TRUE : KeywordType.FALSE,span));
            }
            case Token t -> throw new ParseException("invalid factor " + t);
        };
    }

    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }
}
