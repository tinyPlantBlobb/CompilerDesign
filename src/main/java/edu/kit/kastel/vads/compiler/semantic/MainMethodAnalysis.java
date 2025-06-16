package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ParseException;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;


class MainMethodAnalysis implements NoOpVisitor<MainMethodAnalysis.MainMethodState> {
    private static final String MAIN_METHOD_NAME = "main";
        @Override
        public Unit visit(ProgramTree tree, MainMethodState data) {
            if (tree.topLevelTrees().stream().anyMatch(f->f.name().name().asString().equals(MAIN_METHOD_NAME))) {
                FunctionTree functionTree = tree.topLevelTrees().get(
                        tree.topLevelTrees().indexOf(
                                tree.topLevelTrees().stream()
                                        .filter(f -> f.name().name().asString().equals(MAIN_METHOD_NAME))
                                        .findFirst()
                                        .orElseThrow(() -> new ParseException("No main method found"))
                        )
                );
                if (functionTree.returnType().type().equals(BasicType.INT)) {
                    data.hasMainMethod = true;
                }else {
                    throw new ParseException("Main method must return int, but found: " + functionTree.returnType().type());}
            }
            else
                throw new ParseException("No main method found");

            return NoOpVisitor.super.visit(tree, data);
        }

    static  class MainMethodState {
        boolean hasMainMethod = false;
    }


//
//    @Override
//    public Unit visit(ProgramTree programTree, Unit data) {
//        List<FunctionTree> trees = programTree.topLevelTrees();
//        if (trees.stream().noneMatch(f -> f.name().name()
//                .asString().equals(MAIN_METHOD_NAME))) {
//            throw new SemanticException("No main method found");
//        }
//        return NoOpVisitor.super.visit(programTree, data);
//    }

}
