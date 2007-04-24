/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.classLoader;

import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.shrikeBT.BytecodeConstants;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * Simple object that represents a static call site (ie., an invoke instruction
 * in the bytecode)
 * 
 * Note that the meaning of a call site reference depends on two things:
 * the program counter, and the containing IR.   Thus, it suffices to
 * defines equals() and hashCode() from ProgramCounter, since this class
 * does not maintain a pointer to the containing IR (or CGNode) anyway.
 * If using a hashtable of CallSiteReference from different IRs,
 * you probably want to use a wrapper which also holds a pointer to 
 * the governing CGNode.
 * 
 * @author sfink
 */
public abstract class CallSiteReference extends ProgramCounter implements BytecodeConstants, ContextItem {

  final private MethodReference declaredTarget;

  /**
   * @param programCounter
   * @param declaredTarget
   */
  protected CallSiteReference(int programCounter, MethodReference declaredTarget) {
    super(programCounter);
    this.declaredTarget = declaredTarget;
  }

  // the following atrocities are needed to save a word of space by
  // declaring these classes static, so they don't keep a pointer
  // to the enclosing environment
  // Java makes you type!
  static class StaticCall extends CallSiteReference {
    StaticCall(int programCounter, MethodReference declaredTarget) {
      super(programCounter, declaredTarget);
    }

    public IInvokeInstruction.IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.STATIC;
    }
  }

  static class SpecialCall extends CallSiteReference {
    SpecialCall(int programCounter, MethodReference declaredTarget) {
      super(programCounter, declaredTarget);
    }

    public IInvokeInstruction.IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.SPECIAL;
    }
  }

  static class VirtualCall extends CallSiteReference {
    VirtualCall(int programCounter, MethodReference declaredTarget) {
      super(programCounter, declaredTarget);
    }

    public IInvokeInstruction.IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.VIRTUAL;
    }
  }

  static class InterfaceCall extends CallSiteReference {
    InterfaceCall(int programCounter, MethodReference declaredTarget) {
      super(programCounter, declaredTarget);
    }

    public IInvokeInstruction.IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.INTERFACE;
    }
  }

  /**
   * This factory method plays a little game to avoid storing the invocation
   * code in the object; this saves a byte (probably actually a whole word) in
   * each created object.
   * 
   * TODO: Consider canonicalization?
   */
  public static CallSiteReference make(int programCounter, MethodReference declaredTarget,
      IInvokeInstruction.IDispatch invocationCode) {

    if (invocationCode == IInvokeInstruction.Dispatch.SPECIAL) 
      return new SpecialCall(programCounter, declaredTarget);
    if (invocationCode == IInvokeInstruction.Dispatch.VIRTUAL)
      return new VirtualCall(programCounter, declaredTarget);
    if (invocationCode == IInvokeInstruction.Dispatch.INTERFACE)
      return new InterfaceCall(programCounter, declaredTarget);
    if (invocationCode == IInvokeInstruction.Dispatch.STATIC)
      return new StaticCall(programCounter, declaredTarget);

    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * Return the Method that this call site calls. This represents the method
   * declared in the invoke instruction only.
   */
  public MethodReference getDeclaredTarget() {
    return declaredTarget;
  }

  /**
   * Return one of INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL, or
   * INVOKEINTERFACE
   */
  abstract public IInvokeInstruction.IDispatch getInvocationCode();

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "invoke" + getInvocationString(getInvocationCode()) + " " + declaredTarget + "@" + getProgramCounter();
  }

  /**
   * Method getInvocationString.
   * 
   * @param invocationCode
   * @return String
   */
  protected String getInvocationString(IInvokeInstruction.IDispatch invocationCode) {
    if (invocationCode == IInvokeInstruction.Dispatch.STATIC)
      return "static";
    if (invocationCode == IInvokeInstruction.Dispatch.SPECIAL)
      return "special";
    if (invocationCode == IInvokeInstruction.Dispatch.VIRTUAL)
      return "virtual";
    if (invocationCode == IInvokeInstruction.Dispatch.INTERFACE)
      return "interface";

    Assertions.UNREACHABLE();
    return null;
  }

  public String getInvocationString() {
    return getInvocationString(getInvocationCode());
  }

  /**
   * Is this an invokeinterface call site?
   */
  public final boolean isInterface() {
    return (getInvocationCode() == IInvokeInstruction.Dispatch.INTERFACE);
  }

  /**
   * Is this an invokevirtual call site?
   */
  public final boolean isVirtual() {
    return (getInvocationCode() == IInvokeInstruction.Dispatch.VIRTUAL);
  }

  /**
   * Is this an invokespecial call site?
   */
  public final boolean isSpecial() {
    return (getInvocationCode() == IInvokeInstruction.Dispatch.SPECIAL);
  }

  /**
   * Is this an invokestatic call site?
   */
  public boolean isStatic() {
    return (getInvocationCode() == IInvokeInstruction.Dispatch.STATIC);
  }

  public boolean isFixed() {
    return isStatic() || isSpecial();
  }

  public boolean isDispatch() {
    return isVirtual() || isInterface();
  }
}
