package ibex.lr;

import ibex.lr.GLRNonterminal.Kind;

import java.util.Iterator;
import java.util.List;

class GLRRule {
    List<GLRSymbol> rhs;
    protected GLRNonterminal lhs;
    protected int index;
    protected int prec; // larger means LOWER precedence
    protected Assoc assoc;
    
    enum Assoc { LEFT, RIGHT, NONE };
    
    GLRRule(GLRNonterminal lhs, List<GLRSymbol> rhs, int index) {
        this.lhs = lhs;
        this.index = index;
        this.rhs = rhs;
    }

    GLRRule(GLRNonterminal lhs, List<GLRSymbol> rhs, int index, Object kind) {
        this(lhs, rhs, index);
    }
    
    List<GLRSymbol> symbols() {
        return rhs();
    }
    
    protected List<GLRSymbol> rhs() {
        return rhs;
    }

    GLRRule copy() {
        return new GLRRule(lhs, rhs, index, null);
    }
    
    public int encode() {
        if (lhs.kind == Kind.NORMAL)
            return (lhs.index() << 8) | rhs.size();
        else
            return (lhs.index() << 8) | 0;
    }

    public String toString() {
        String s = index + ": " + lhs + " ::=";
        for (Iterator<GLRSymbol> i = rhs.iterator(); i.hasNext();) {
            GLRSymbol y = (GLRSymbol) i.next();
            s += " " + y;
        }
        return s;
    }

    protected GLRNonterminal lhs() {
        return lhs;
    }

    protected int index() {
        return index;
    }

    Action action() {
        return null;
    }

    public boolean equals(Object o) {
        if (o instanceof GLRRule) {
            GLRRule r = (GLRRule) o;
            return index == r.index;
        }
        return false;
    }

    public int hashCode() {
        return index;
    }
}
