package de.fhg.iais.roberta.syntax.lang.expr;

import de.fhg.iais.roberta.syntax.BlockType;
import de.fhg.iais.roberta.syntax.BlocklyBlockProperties;
import de.fhg.iais.roberta.syntax.BlocklyComment;
import de.fhg.iais.roberta.syntax.Phrase;
import de.fhg.iais.roberta.transformer.NepoPhrase;
import de.fhg.iais.roberta.typecheck.BlocklyType;
import de.fhg.iais.roberta.util.dbc.DbcException;

/**
 * the top class of all expressions. There are two ways for a client to find out which kind of expression an {@link #Expr}-object is:<br>
 * - {@link #getKind()}<br>
 * - {@link #getAs(Class)}<br>
 */
public abstract class Expr<V> extends Phrase<V> {

    /**
     * create a mutable expression of the given {@link BlockType}
     *
     * @param kind the kind of the expression,
     * @param properties of the block (see {@link BlocklyBlockProperties}),
     * @param comment of the user for the specific block
     */
    public Expr(BlockType kind, BlocklyBlockProperties properties, BlocklyComment comment) {
        super(kind, properties, comment);
    }

    /**
     * get the precedence of the expression
     * <b>This is the default implementation of annotated AST classes</b>
     *
     * @return the precedence
     */
    public int getPrecedence() {
        NepoPhrase classAnno = this.getClass().getAnnotation(NepoPhrase.class);
        if ( classAnno == null ) {
            throw new DbcException("the default implementation of getPrecedence() fails with the NOT annotated class " + this.getClass().getSimpleName());
        }
        return classAnno.precedence();
    }

    /**
     * get the association of the expression
     * <b>This is the default implementation of annotated AST classes</b>
     *
     * @return the association
     */
    public Assoc getAssoc() {
        NepoPhrase classAnno = this.getClass().getAnnotation(NepoPhrase.class);
        if ( classAnno == null ) {
            throw new DbcException("the default implementation of getAssoc() fails with the NOT annotated class " + this.getClass().getSimpleName());
        }
        return classAnno.assoc();
    }

    /**
     * get the BlocklyType (used for typechecking ...) of this expression
     * <b>This is the default implementation of annotated AST classes</b>
     *
     * @return the BlocklyType
     */
    public BlocklyType getVarType() {
        NepoPhrase classAnno = this.getClass().getAnnotation(NepoPhrase.class);
        if ( classAnno == null ) {
            throw new DbcException("the default implementation of getVarType() fails with the NOT annotated class " + this.getClass().getSimpleName());
        }
        return classAnno.blocklyType();
    }

}
