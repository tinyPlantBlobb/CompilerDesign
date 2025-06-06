package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.OptionalLong;

public record LiteralIntTree(String value, int base, Span span) implements LiteralTree<OptionalLong> {

    private static final BasicType type = BasicType.INT;


    @Override
    public OptionalLong parseValue() {
      int begin = 0;
      int end = value.length();
      if (base == 16) {
        begin = 2; // ignore 0x
      }
      long l;
      try {
        l = Long.parseLong(value.substring(begin, end), base);
      } catch (NumberFormatException e) {
        return OptionalLong.empty();
      }
      boolean isNegative = l < 0;
      boolean validPositiveInt = l <= Integer.toUnsignedLong(Integer.MIN_VALUE);
      boolean validInt = l <= Integer.toUnsignedLong(-1);
      if (isNegative || (!validPositiveInt && base != 16) || !validInt) {
        return OptionalLong.empty();
      }
      return OptionalLong.of(l);
    }

    public BasicType getType() {
      return type;
    }

    @Override
    public Span span() {
      return span;
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
      return visitor.visit(this, data);
    }


    private OptionalLong parseDec(int end) {
      long l;
      try {
        l = Long.parseLong(value, 0, end, base);
      } catch (NumberFormatException _) {
        return OptionalLong.empty();
      }
      if (l < 0 || l > Integer.toUnsignedLong(Integer.MIN_VALUE)) {
        return OptionalLong.empty();
      }
      return OptionalLong.of(l);
    }

    private OptionalLong parseHex(int end) {
      try {
        return OptionalLong.of(Integer.parseUnsignedInt(value, 2, end, 16));
      } catch (NumberFormatException e) {
        return OptionalLong.empty();
      }
    }
  }
