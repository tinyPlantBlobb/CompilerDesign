package edu.kit.kastel.vads.compiler;

import edu.kit.kastel.vads.compiler.backend.x86.x86CodeGenerator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.SsaTranslation;
import edu.kit.kastel.vads.compiler.ir.optimize.ConstantFolding;
//import edu.kit.kastel.vads.compiler.ir.optimize.LocalValueNumbering;
import edu.kit.kastel.vads.compiler.ir.util.YCompPrinter;
import edu.kit.kastel.vads.compiler.lexer.Lexer;
import edu.kit.kastel.vads.compiler.parser.ParseException;
import edu.kit.kastel.vads.compiler.parser.Parser;
import edu.kit.kastel.vads.compiler.parser.TokenSource;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.semantic.SemanticAnalysis;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.println("Invalid arguments: Expected one input file and one output file");
      System.exit(3);
    }
    Path input = Path.of(args[0]);
    Path output = Path.of(args[1]);
    ProgramTree program = lexAndParse(input);
    try {
      new SemanticAnalysis(program).analyze();
    } catch (SemanticException e) {
      e.printStackTrace();
      System.exit(7);

      return;
    } catch (ParseException e) {
        e.printStackTrace();
        System.exit(42);
        return;
    }
    List<IrGraph> graphs = new ArrayList<>();
    for (FunctionTree function : program.topLevelTrees()) {
      SsaTranslation translation = new SsaTranslation(function, new ConstantFolding());
      graphs.add(translation.translate());

    }
    if ("vcg".equals(System.getenv("DUMP_GRAPHS")) || "vcg".equals(System.getProperty("dumpGraphs"))) {
      Path tmp = output.toAbsolutePath().resolveSibling("graphs");
      Files.createDirectory(tmp);
      for (IrGraph graph : graphs) {
        dumpGraph(graph, tmp, "before-codegen");
      }
    }

    // assembly
    String s = new x86CodeGenerator().generateCode(graphs);
    Path assemblyFilePath = Path.of(output+".s");

    Files.writeString(assemblyFilePath, s);

 //Build and start gcc process
    ProcessBuilder builder = new ProcessBuilder(
            "gcc",
            assemblyFilePath.toString(),
            "-o",
            output.toString()
    );
    Process process = builder.start();

    // Wait for gcc to finish
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      String error = new String(process.getErrorStream().readAllBytes());
      throw new Exception("gcc assembly failed: " + error);
    }

  }

  private static ProgramTree lexAndParse(Path input) throws IOException {
    try {
      Lexer lexer = Lexer.forString(Files.readString(input));
      TokenSource tokenSource = new TokenSource(lexer);
      Parser parser = new Parser(tokenSource);
      return parser.parseProgram();
    } catch (ParseException e) {
      e.printStackTrace();
      System.exit(42);
      throw new AssertionError("unreachable");
    }
  }

  private static void dumpGraph(IrGraph graph, Path path, String key) throws IOException {
    Files.writeString(
        path.resolve(graph.name() + "-" + key + ".vcg"),
        YCompPrinter.print(graph));
  }
}
