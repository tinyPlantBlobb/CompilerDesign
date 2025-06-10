package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.visitor.RecursivePostorderVisitor;

import java.util.LinkedList;

public class SemanticAnalysis {

  private final ProgramTree program;

  public SemanticAnalysis(ProgramTree program) {
    this.program = program;
  }

  public void analyze() {
    this.program.accept(new RecursivePostorderVisitor<>(new IntegerLiteralRangeAnalysis()), new Namespace<>());
    this.program.accept(new RecursivePostorderVisitor<>(new VariableStatusAnalysis()), new Namespace<>());
    this.program.accept(new RecursivePostorderVisitor<>(new TypeCheckingAnalysis()), new LinkedList<>());
    this.program.accept(new RecursivePostorderVisitor<>(new ReturnAnalysis()), new ReturnAnalysis.ReturnState());
    this.program.accept(new RecursivePostorderVisitor<>(new MainMethodAnalysis()), new MainMethodAnalysis.MainMethodState());
  }

}
