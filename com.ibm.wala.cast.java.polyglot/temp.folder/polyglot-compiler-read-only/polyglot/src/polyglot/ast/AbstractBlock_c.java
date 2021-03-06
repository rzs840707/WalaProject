/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.*;

import polyglot.types.Context;
import polyglot.util.*;
import polyglot.visit.*;

/**
 * A <code>Block</code> represents a Java block statement -- an immutable
 * sequence of statements.
 */
public abstract class AbstractBlock_c extends Stmt_c implements Block
{
    protected List<Stmt> statements;

    public AbstractBlock_c(Position pos, List<Stmt> statements) {
	super(pos);
	assert(statements != null);
	this.statements = TypedList.copyAndCheck(statements, Stmt.class, true);
    }

    /** Get the statements of the block. */
    public List<Stmt> statements() {
	return this.statements;
    }

    /** Set the statements of the block. */
    public Block statements(List<Stmt> statements) {
	AbstractBlock_c n = (AbstractBlock_c) copy();
	n.statements = TypedList.copyAndCheck(statements, Stmt.class, true);
	return n;
    }

    /** Append a statement to the block. */
    public Block append(Stmt stmt) {
	List<Stmt> l = new ArrayList<Stmt>(statements.size()+1);
	l.addAll(statements);
	l.add(stmt);
	return statements(l);
    }

    /** Prepend a statement to the block. */
    public Block prepend(Stmt stmt) {
        List<Stmt> l = new ArrayList<Stmt>(statements.size()+1);
        l.add(stmt);
        l.addAll(statements);
        return statements(l);
    }

    /** Reconstruct the block. */
    protected AbstractBlock_c reconstruct(List<Stmt> statements) {
	if (! CollectionUtil.allEqual(statements, this.statements)) {
	    AbstractBlock_c n = (AbstractBlock_c) copy();
	    n.statements = TypedList.copyAndCheck(statements, Stmt.class, true);
	    return n;
	}

	return this;
    }

    /** Visit the children of the block. */
    public Node visitChildren(NodeVisitor v) {
        List<Stmt> statements = visitList(this.statements, v);
	return reconstruct(statements);
    }

    public Context enterScope(Context c) {
	return c.pushBlock();
    }

    /** Write the block to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	w.begin(0);

	for (Iterator<Stmt> i = statements.iterator(); i.hasNext(); ) {
	    Stmt n = i.next();
	    
	    printBlock(n, w, tr);

	    if (i.hasNext()) {
		w.newline();
	    }
	}

	w.end();
    }

    public Term firstChild() {
        return listChild(statements, null);
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFGList(statements, this, EXIT);
        return succs;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{");

        int count = 0;

        for (Iterator<Stmt> i = statements.iterator(); i.hasNext(); ) {
            if (count++ > 2) {
                sb.append(" ...");
                break;
            }

            Stmt n = i.next();
            sb.append(" ");
            sb.append(n.toString());
        }

        sb.append(" }");
        return sb.toString();
    }
}
