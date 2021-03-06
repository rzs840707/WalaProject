/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;

/**
 * A <code>CanonicalTypeNode</code> is a type node for a canonical type.
 */
public class CanonicalTypeNode_c extends TypeNode_c implements CanonicalTypeNode
{
  public CanonicalTypeNode_c(Position pos, Ref<? extends Type> type) {
    super(pos);
    assert(type != null);
    this.type = type;
  }
  
  public CanonicalTypeNode typeRef(Ref<? extends Type> type) {
      return (CanonicalTypeNode) super.typeRef(type);
  }
  
  /** Type check the type node.  Check accessibility of class types. */
  public Node typeCheck(ContextVisitor tc) throws SemanticException {
      TypeSystem ts = tc.typeSystem();

      if (type.get().isClass()) {
          ClassType ct = type.get().toClass();
          if (ct.isTopLevel() || ct.isMember()) {
              if (! ts.classAccessible(ct.def(), tc.context())) {
                  throw new SemanticException("Cannot access class \"" +
                      ct + "\" from the body of \"" +
                      tc.context().currentClass() + "\".", position());
              }
          }
      }

      return this;
  }
  
  public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
    if (type == null) {
        w.write("<unknown-type>");
    } else {
        type.get().print(w);
    }
  }
  
  /**
   * Translate the type.
   * If the "use-fully-qualified-class-names" options is used, then the
   * fully qualified names is written out (<code>java.lang.Object</code>).
   * Otherwise, the string that originally represented the type in the
   * source file is used.
   */
  public void translate(CodeWriter w, Translator tr) {
      w.write(type.get().translate(tr.context()));
  }

  public String toString() {
    if (type == null) return "<unknown-type>";
    return type.toString();
  }

  public void dump(CodeWriter w) {
    super.dump(w);
    w.allowBreak(4, " ");
    w.begin(0);
    w.write("(type " + type + ")");
    w.end();
  }
}
