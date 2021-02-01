package de.fhg.iais.roberta.syntax.lang.expr;

import de.fhg.iais.roberta.syntax.*;
import de.fhg.iais.roberta.transformer.NepoComponent;
import de.fhg.iais.roberta.transformer.NepoPhrase;
import de.fhg.iais.roberta.typecheck.BlocklyType;

/**
 * This class represents the <b>robColour_rgb</b> block from Blockly
 */
@NepoPhrase(containerType = "RGB_COLOR", blocklyType = BlocklyType.COLOR, precedence = 999, assoc = Assoc.NONE)
public class RgbColor<V> extends Expr<V> {
    @NepoComponent(fieldName = BlocklyConstants.RED, fieldType = BlocklyType.NUMBER_INT)
    public final Expr<V> R;
    @NepoComponent(fieldName = BlocklyConstants.GREEN, fieldType = BlocklyType.NUMBER_INT)
    public final Expr<V> G;
    @NepoComponent(fieldName = BlocklyConstants.BLUE, fieldType = BlocklyType.NUMBER_INT)
    public final Expr<V> B;
    @NepoComponent(fieldName = BlocklyConstants.ALPHA, fieldType = BlocklyType.NUMBER_INT)
    public final Expr<V> A;

    public RgbColor(BlockType kind, BlocklyBlockProperties properties, BlocklyComment comment) {
        super(kind, properties, comment);
        R = G = B = A = null;
        setReadOnly();
    }

    // for textly. TODO: make it better
    public RgbColor(BlocklyBlockProperties properties, BlocklyComment comment, Expr<V> r, Expr<V> g, Expr<V> b, Expr<V> a) {
        super(BlockTypeContainer.getByName("RGB_COLOR"), properties, comment);
        R = r;
        G = g;
        B = b;
        A = a;
        setReadOnly();
    }

    // TODO: inline, if proposal is accepted
    public Expr<V> getR() {
        return R;
    }

    // TODO: inline, if proposal is accepted
    public Expr<V> getG() {
        return G;
    }

    // TODO: inline, if proposal is accepted
    public Expr<V> getB() {
        return B;
    }

    // TODO: inline, if proposal is accepted
    public Expr<V> getA() {
        return A;
    }
}
