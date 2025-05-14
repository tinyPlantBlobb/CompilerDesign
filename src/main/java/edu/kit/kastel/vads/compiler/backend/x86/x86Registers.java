package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public sealed interface x86Registers extends Register permits x86Registers.RealRegisters, x86Registers.OverflowRegisters {
  enum RealRegisters implements x86Registers {

    RAX,
    RBX,
    RCX,
    RDX,
    RSI,
    RDI,
    RSP,
    RBP,
    R8,
    R9,
    R10,
    R11,
    R12,
    R13,
    R14,
    R15;


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


