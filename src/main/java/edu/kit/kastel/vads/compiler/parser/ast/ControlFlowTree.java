package edu.kit.kastel.vads.compiler.parser.ast;

public sealed interface ControlFlowTree extends StatementTree permits IfTree, WhileTree, ForTree, ReturnTree, BreakTree, ContinueTree {

}
