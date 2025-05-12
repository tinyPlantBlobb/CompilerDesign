package edu.kit.kastel.vads.compiler.backend.x84;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public interface x84Registers extends Register {
  public enum RealRegisters implements x84Registers {
    EAX,
    EBX,
    ECX,
    EDX,
    ESI,
    EDI,
    RSP,
    RBP,
    R8D,
    R9D,
    R10D,
    R11D,
    R12D,
    R13D,
    R14D,
    R15D;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

}
