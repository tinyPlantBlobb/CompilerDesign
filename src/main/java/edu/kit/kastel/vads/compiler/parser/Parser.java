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
        // add stuff ofr functions later here
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
            System.out.println("Parsed decl: " + statement);
        } else if (this.tokenSource.peek().isControlFlow()) {
            statement = parseControlFlow();
            System.out.println("Parsed ctrl f: " + statement);
        }
        else if (this.tokenSource.peek().isSeparator(SeparatorType.BRACE_OPEN)) {
            statement = parseBlock();
            System.out.println("Parsed block: " + statement);
        } else {
            statement = parseSimple();
            System.out.println("Parsed simple statement: " + statement);
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


    private ExpressionTree parseExpression() {
        ExpressionTree lhs = parsePrecedenceExpression(OperatorType.MAX_PRECEDENCE);
        System.out.println("Expression lhs "+lhs+ " token: " + this.tokenSource.peek());
            if (this.tokenSource.peek().isOperator(OperatorType.TERNARY_CONDITION) ) {
                    this.tokenSource.consume();
                    ExpressionTree trueExpression = parseExpression();
                    this.tokenSource.expectOperator(OperatorType.TERNARY_COLON);
                    ExpressionTree falseExpression = parseExpression();
                    return new TernaryOperationTree(lhs, trueExpression, falseExpression);
                }
            else {
                System.out.println("returnning Expression lhs "+lhs);
                return lhs;
            }

    }
    private ExpressionTree parsePrecedenceExpression(int precedence) {

        if (precedence == OperatorType.UNARY_PRECEDENCE) {
            return parseUnaryExpression(precedence);
        } else if (precedence == 0) {
            return parseBasicExpression();
        }
        ExpressionTree lhs = parsePrecedenceExpression(precedence - 1);
        while (true) {
            System.out.println("next precedence" +this.tokenSource.peek());
            if (this.tokenSource.peek() instanceof Operator(var type, _)
                && type.precedence().contains(precedence)) {
                this.tokenSource.consume();
                System.out.println("Parsing binary operation with precedence " + precedence + " and type " + type);
                lhs = new BinaryOperationTree(lhs, parsePrecedenceExpression(precedence - 1), type);
                System.out.println(Printer.print(lhs));
            } else {
                return lhs;

            }
        }
    }
    private ExpressionTree parseBasicExpression() {
        Token next = this.tokenSource.peek();
        switch (next) {
            case Separator(var type, _) when next.isSeparator(SeparatorType.PAREN_OPEN) -> {
                this.tokenSource.consume();
                ExpressionTree expression = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                return expression;
            }
            case Identifier ident -> {
                this.tokenSource.consume();
                return new IdentExpressionTree(name(ident));
            }
            case NumberLiteral(String value, int base, Span span) -> {
                this.tokenSource.consume();
                return new LiteralIntTree(value, base, span);
            }
            case Keyword(var type, Span span) when type == KeywordType.TRUE || type == KeywordType.FALSE -> {
                this.tokenSource.consume();
                return new LiteralBoolTree(new Keyword(type, span));
            }
            default -> throw new ParseException("expected basic expression but got " + next);
        }
    }
    private ExpressionTree parseUnaryExpression(int precedence) {
        System.out.println("Unary Expression first token" +this.tokenSource.peek());
        if (this.tokenSource.peek() instanceof Operator(var type, _)
            && type.precedence().contains(precedence)) {
            Operator token = this.tokenSource.expectOperator(type);
            this.tokenSource.consume();
            ExpressionTree operand = parsePrecedenceExpression(precedence);
            return new UnaryOperationTree(token, operand);
        } else {
            return parsePrecedenceExpression(precedence-1);
        }
    }

    private StatementTree parseSimpop() {
        Token seperator = this.tokenSource.peek();
        if ((seperator instanceof Separator(var type, _))) {
            return null;
        } else {
            if (seperator.isKeyword(KeywordType.INT)||seperator.isKeyword(KeywordType.BOOL)) {
               return parseDeclaration();
            } else {
               return parseSimple();
            }
        }
    }

    private ControlFlowTree parseControlFlow() {
        Token token = this.tokenSource.peek();
        System.out.println("Control flow token: " + token);
        switch (token) {
            case Keyword(var type, Span span) when type == KeywordType.IF -> {
                this.tokenSource.consume();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
                ExpressionTree condition = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                StatementTree thenTree = parseStatement();

                if (this.tokenSource.peek().isKeyword(KeywordType.ELSE)) {
                    this.tokenSource.expectKeyword(KeywordType.ELSE);
                    StatementTree elseTree = parseStatement();
                    return new IfTree(condition, thenTree, elseTree, span.start());
                }
                else {
                    return new IfTree(condition, thenTree, null, span.start());
                }

            }
            case Keyword(var type, Span span) when type == KeywordType.WHILE -> {
                this.tokenSource.expectKeyword(KeywordType.WHILE);
                this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
                ExpressionTree condition = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                StatementTree body = parseStatement();
                return new WhileTree(condition, body, span.start());
            }
            case Keyword(var type, Span span) when type == KeywordType.FOR -> {

                this.tokenSource.consume();

                this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
                System.out.println("For token: " + this.tokenSource.peek());
                StatementTree decl =  parseSimpop();
                System.out.println("For loop decl "+decl);
                this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
                ExpressionTree condition = parseExpression();
                System.out.println("condition node "+condition);
                this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
                StatementTree step = parseStatement();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                StatementTree body = parseStatement();

                return new ForTree(decl, condition, step, body, span.start());
            }
            case Keyword(var type, Span span) when type == KeywordType.BREAK -> {
                this.tokenSource.expectKeyword(KeywordType.BREAK);
                this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
                return new BreakTree(span);
            }
            case Keyword(var type, Span span) when type == KeywordType.CONTINUE -> {
                this.tokenSource.expectKeyword(KeywordType.CONTINUE);
                this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
                return new ContinueTree(span);
            }
            case Keyword(var type, Span span) when type == KeywordType.RETURN -> {
                this.tokenSource.expectKeyword(KeywordType.RETURN);
                ExpressionTree expression = parseExpression();
                return new ReturnTree(expression, span.start());
            }
            default -> throw new ParseException("expected control flow but got " + token);
        }

    }



    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }
}
