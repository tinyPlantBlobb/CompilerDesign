package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;


  public enum x86Registers implements Register{
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


