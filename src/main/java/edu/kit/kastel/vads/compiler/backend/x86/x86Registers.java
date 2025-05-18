package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public sealed interface x86Registers extends Register permits x86Registers.RealRegisters, x86Registers.OverflowRegisters {
  enum RealRegisters implements x86Registers {

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
  final class OverflowRegisters implements x86Registers {
    public int id;
    public OverflowRegisters(int id) {
      this.id = id;
    }
    @Override
    public String toString() {
      return "DWORD PTR["+RealRegisters.RSP+" + " + id*4+ "]";
    }

  }


}


