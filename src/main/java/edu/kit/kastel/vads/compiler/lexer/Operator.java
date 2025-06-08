package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.type.Type;

import java.util.List;

public record Operator(OperatorType type, Span span) implements Token {

    @Override
    public boolean isOperator(OperatorType operatorType) {
        return type() == operatorType;
    }

    @Override
    public String asString() {
        return type().toString();
    }




    public enum OperatorType(String value, List<Integer> precedence) {
        LOGICAL_AND("&&", List.of(10)),
        LOGICAL_OR("||", List.of(11)),
        LOGICAL_NOT("!", List.of(1)),
        BITWISE_AND("&", List.of(7)),
        BITWISE_OR("|", List.of(9)),
        BITWISE_XOR("^", List.of(8)),
        BITWISE_NOT("~", List.of(1)),
        LEFT_SHIFT("<<", List.of(4)),
        RIGHT_SHIFT(">>", List.of(4)),

        GREATER_EQUAL(">=", List.of(5)),
        LESS_EQUAL("<=", List.of(5)),
        GREATER_THAN(">", List.of(5)),
        LESS_THAN("<", List.of(5)),

        LOGICAL_EQUAL("==", List.of(6)),
        LOGICAL_NOT_EQUAL("!=", List.of(6)),

        ASSIGN_MINUS("-=", List.of()),
        MINUS("-", List.of(1,3)),
        ASSIGN_PLUS("+=", List.of()),
        PLUS("+", List.of(3)),
        MUL("*" , List.of(2)),
        ASSIGN_MUL("*=", List.of()),
        ASSIGN_DIV("/=", List.of()),
        DIV("/", List.of(2)),
        ASSIGN_MOD("%=", List.of()),
        MOD("%", List.of(2)),
        ASSIGN("=", List.of()),

        ASSIGN_LEFT_SHIFT("<<=", List.of()),
        ASSIGN_RIGHT_SHIFT(">>=", List.of()),
        ASSIGN_BITWISE_AND("&=", List.of()),
        ASSIGN_BITWISE_OR("|=", List.of()),
        ASSIGN_BITWISE_XOR("^=", List.of()),

        TERNARY_CONDITION("?", List.of()),
        TERNARY_COLON(":", List.of())
        ;


        OperatorType(String value, List<Integer> precedence) {
        }

        public Type inputType() {
            return switch (this) {
                case LOGICAL_EQUAL, LOGICAL_NOT_EQUAL -> null;
                case LOGICAL_AND, LOGICAL_OR, LOGICAL_NOT -> BasicType.BOOL;
                case MINUS, PLUS, MUL, DIV, MOD, LEFT_SHIFT, RIGHT_SHIFT, BITWISE_AND, BITWISE_NOT,
                     BITWISE_OR, BITWISE_XOR, GREATER_EQUAL, GREATER_THAN, LESS_EQUAL, LESS_THAN -> BasicType.INT;
                case ASSIGN_MINUS -> MINUS.inputType();
                case ASSIGN_PLUS -> PLUS.inputType();
                case ASSIGN_MUL -> MUL.inputType();
                case ASSIGN_DIV -> DIV.inputType();
                case ASSIGN_MOD -> MOD.inputType();
                case ASSIGN_LEFT_SHIFT -> LEFT_SHIFT.inputType();
                case ASSIGN_RIGHT_SHIFT -> RIGHT_SHIFT.inputType();
                case ASSIGN_BITWISE_AND -> BITWISE_AND.inputType();
                case ASSIGN_BITWISE_OR -> BITWISE_OR.inputType();
                case ASSIGN_BITWISE_XOR -> BITWISE_XOR.inputType();
                case ASSIGN, TERNARY_COLON, TERNARY_CONDITION ->
                        throw new IllegalStateException("Assignment operator does not have an input type that is able to be determined");
                default -> throw new IllegalStateException("Unexpected operator type: " + this.toString());

            };
        }
        public Type outputType() {
            return switch (this) {
                case LOGICAL_EQUAL, LOGICAL_NOT_EQUAL, LOGICAL_AND, LOGICAL_OR, LOGICAL_NOT -> BasicType.BOOL;
                case MINUS, PLUS, MUL, DIV, MOD, LEFT_SHIFT, RIGHT_SHIFT, BITWISE_AND, BITWISE_NOT,
                     BITWISE_OR, BITWISE_XOR, GREATER_EQUAL, GREATER_THAN, LESS_EQUAL, LESS_THAN -> BasicType.INT;
                case ASSIGN_MINUS, ASSIGN_PLUS , ASSIGN_MUL ,ASSIGN_DIV , ASSIGN_MOD ,ASSIGN_LEFT_SHIFT , ASSIGN_RIGHT_SHIFT ,ASSIGN_BITWISE_AND , ASSIGN_BITWISE_OR , ASSIGN_BITWISE_XOR , ASSIGN, TERNARY_COLON, TERNARY_CONDITION ->
                        throw new IllegalStateException("Assignment operator does not have an output type that is able to be determined");
                default -> throw new IllegalStateException("Unexpected operator type: " + this.toString());
            };
        }

        @Override
        public String toString() {
            return this.value;
        }

    }
}
