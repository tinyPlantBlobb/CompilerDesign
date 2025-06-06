package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.OptionalLong;

public sealed interface LiteralTree<T> extends ExpressionTree permits LiteralBoolTree , LiteralIntTree {

  @Override
  default <T, R> R accept(Visitor<T, R> visitor, T data) {
    throw new UnsupportedOperationException("accept must be implemented in subclasses");
  }

  T parseValue();
}

