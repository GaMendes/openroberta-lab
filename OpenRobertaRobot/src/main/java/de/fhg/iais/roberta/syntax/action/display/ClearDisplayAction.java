package de.fhg.iais.roberta.syntax.action.display;

import java.util.List;

import de.fhg.iais.roberta.blockly.generated.Block;
import de.fhg.iais.roberta.blockly.generated.Field;
import de.fhg.iais.roberta.factory.BlocklyDropdownFactory;
import de.fhg.iais.roberta.syntax.*;
import de.fhg.iais.roberta.syntax.action.Action;
import de.fhg.iais.roberta.syntax.lang.expr.Assoc;
import de.fhg.iais.roberta.transformer.*;
import de.fhg.iais.roberta.typecheck.BlocklyType;

/**
 * This class represents the <b>robActions_display_clear</b> block from Blockly into the AST (abstract syntax tree).
 */
@NepoPhrase(containerType = "CLEAR_DISPLAY_ACTION")
public final class ClearDisplayAction<V> extends Action<V> {
    @NepoComponent(fieldName = BlocklyConstants.ACTORPORT, isFieldWithDefault = BlocklyConstants.NO_PORT)
    public final String port;

    public ClearDisplayAction(BlockType kind, BlocklyBlockProperties properties, BlocklyComment comment) {
        super(kind, properties, comment);
        this.port = null;
        setReadOnly();
    }

    // TODO: inline, if proposal is accepted
    public String getPort() {
        return this.port;
    }

    // TODO: remove, if Transformer is better engineered
    public ClearDisplayAction(BlocklyBlockProperties properties, BlocklyComment comment, String port) {
        super(BlockTypeContainer.getByName("CLEAR_DISPLAY_ACTION"), properties, comment);
        this.port = port;
        setReadOnly();
    }

    // TODO: remove, if Transformer is better engineered
    public static <V> ClearDisplayAction<V> make(BlocklyBlockProperties properties, BlocklyComment comment, String port) {
        return new ClearDisplayAction(properties, comment, port);
    }
}
