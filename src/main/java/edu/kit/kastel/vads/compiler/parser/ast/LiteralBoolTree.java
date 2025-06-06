package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Keyword;
import edu.kit.kastel.vads.compiler.lexer.KeywordType;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.OptionalLong;

public record LiteralBoolTree(Keyword value)  implements LiteralTree<Boolean> {

  private static final BasicType type = BasicType.BOOL;

  @Override
  public Boolean parseValue() {
    if (value.type() == KeywordType.TRUE) {
      return true;
    } else if (value.type() == KeywordType.FALSE) {
      return false;
    } else {
      throw new IllegalArgumentException("Invalid keyword type " + value.type() + " for boolean literal");
    }
  }

  public BasicType getType() {
    return type;
  }

  public Span span() {
    return value.span();
  }

  @Override
  public <T, R> R accept(Visitor<T, R> visitor, T data) {
    return visitor.visit(this, data);
  }
}
