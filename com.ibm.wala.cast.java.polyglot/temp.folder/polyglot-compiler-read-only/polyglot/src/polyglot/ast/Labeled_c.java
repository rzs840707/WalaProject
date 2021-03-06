/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.ArrayList;
import java.util.List;

import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;

/**
 * Am immutable representation of a Java statement with a label.  A labeled
 * statement contains the statement being labelled and a string label.
 */
public class Labeled_c extends Stmt_c implements Labeled
{
    protected Id label;
    protected Stmt statement;

    public Labeled_c(Position pos, Id label, Stmt statement) {
	super(pos);
	assert(label != null && statement != null);
	this.label = label;
	this.statement = statement;
    }
    
    /** Get the label of the statement. */
    public Id labelNode() {
        return this.label;
    }
    
    /** Set the label of the statement. */
    public Labeled labelNode(Id label) {
        Labeled_c n = (Labeled_c) copy();
        n.label = label;
        return n;
    }

    /** Get the sub-statement of the statement. */
    public Stmt statement() {
	return this.statement;
    }

    /** Set the sub-statement of the statement. */
    public Labeled statement(Stmt statement) {
	Labeled_c n = (Labeled_c) copy();
	n.statement = statement;
	return n;
    }

    /** Reconstruct the statement. */
    protected Labeled_c reconstruct(Id label, Stmt statement) {
	if (label != this.label || statement != this.statement) {
	    Labeled_c n = (Labeled_c) copy();
            n.label = label;
	    n.statement = statement;
	    return n;
	}

	return this;
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
        Id label = (Id) visitChild(this.label, v);
	Node statement = visitChild(this.statement, v);
        
        if (statement instanceof NodeList) {
          // Return a NodeList of statements, applying the label to the first
          // statement.
          NodeList nl = (NodeList) statement;
          List<Node> stmts = new ArrayList<Node>(nl.nodes());
          
          Stmt first = (Stmt) stmts.get(0);
          first = reconstruct(label, first);
          stmts.set(0, first);
          
          return nl.nodes(stmts);
        }
        
	return reconstruct(label, (Stmt) statement);
    }

    public String toString() {
	return label + ": " + statement;
    }

    /** Write the statement to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	w.write(label + ": ");
	print(statement, w, tr);
    }

    public Term firstChild() {
        return statement;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.push(this).visitCFG(statement, this, EXIT);
        return succs;
    }
    

}
