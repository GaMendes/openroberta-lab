package de.fhg.iais.roberta.transformer;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.iais.roberta.blockly.generated.Block;
import de.fhg.iais.roberta.blockly.generated.BlockSet;
import de.fhg.iais.roberta.blockly.generated.Instance;
import de.fhg.iais.roberta.blockly.generated.Value;
import de.fhg.iais.roberta.components.ProgramAst;
import de.fhg.iais.roberta.factory.IRobotFactory;
import de.fhg.iais.roberta.syntax.*;
import de.fhg.iais.roberta.syntax.lang.blocksequence.Location;
import de.fhg.iais.roberta.syntax.lang.expr.Expr;
import de.fhg.iais.roberta.util.dbc.Assert;
import de.fhg.iais.roberta.util.dbc.DbcException;

/**
 * JAXB to AST transformer. Client should provide tree of jaxb objects.
 */
public class Jaxb2ProgramAst<V> extends AbstractJaxb2Ast<V> {
    private static final Logger LOG = LoggerFactory.getLogger(Jaxb2ProgramAst.class);

    public Jaxb2ProgramAst(IRobotFactory robotFactory) {
        super(robotFactory);
    }

    /**
     * Converts object of type {@link BlockSet} to AST tree.
     *
     * @param set the BlockSet to transform
     */
    public ProgramAst<V> blocks2Ast(BlockSet set) {
        ProgramAst.Builder<V> builder =
            new ProgramAst.Builder<V>()
                .setRobotType(set.getRobottype())
                .setXmlVersion(set.getXmlversion())
                .setDescription(set.getDescription())
                .setTags(set.getTags());

        List<Instance> instances = set.getInstance();
        for ( Instance instance : instances ) {
            builder.addToTree(instanceToAST(instance));
        }
        return builder.build();
    }

    private List<Phrase<V>> instanceToAST(Instance instance) {
        List<Block> blocks = instance.getBlock();
        Location<V> location = Location.make(instance.getX(), instance.getY());
        List<Phrase<V>> range = new ArrayList<>();
        range.add(location);
        for ( Block block : blocks ) {
            range.add(blockToAST(block));
        }
        return range;
    }

    @Override
    protected Phrase<V> blockToAST(Block block) {
        if ( block == null ) {
            throw new DbcException("Invalid null block");
        }
        String type = block.getType().trim().toLowerCase();
        BlockType matchingBlockType = BlockTypeContainer.getByBlocklyName(type);
        Assert.notNull(matchingBlockType, "Invalid Block: " + block.getType());
        return block2phrase(block, matchingBlockType.getAstClass().getName());
    }

    private Phrase<V> block2phrase(Block block, String className) {
        try {
            Class<?> astClass = Class.forName(className);
            NepoPhrase nepoAnnotation = astClass.getAnnotation(NepoPhrase.class);
            if ( nepoAnnotation == null ) {
                return block2phraseWithOldJaxbToAst(block, astClass);
            } else {
                return block2astByAnnotation(block, astClass);
            }
        } catch ( ClassNotFoundException cnfe ) {
            throw new DbcException("Could not load AST class " + className + " . Inspect the block declation YML files of the plugin used");
        }
    }

    @SuppressWarnings("unchecked")
    private Phrase<V> block2phraseWithOldJaxbToAst(Block block, Class<?> astClass) {
        try {
            Method method = astClass.getMethod("jaxbToAst", Block.class, AbstractJaxb2Ast.class);
            return (Phrase<V>) method.invoke(null, block, this);
        } catch ( InvocationTargetException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException e ) {
            throw new DbcException("Could not find or invoke the static method jaxbToAst for AST class " + astClass.getSimpleName(), e);
        }
    }

    private Phrase<V> block2astByAnnotation(Block block, Class<?> astClass) {
        try {
            NepoPhrase classAnno = astClass.getAnnotation(NepoPhrase.class);
            BlockType btc = BlockTypeContainer.getByName(classAnno.containerType());
            BlocklyBlockProperties bp = Jaxb2Ast.extractBlockProperties(block);
            BlocklyComment bc = Jaxb2Ast.extractComment(block);
            Constructor<?> declaredConstructor = astClass.getDeclaredConstructor(BlockType.class, BlocklyBlockProperties.class, BlocklyComment.class);
            @SuppressWarnings("unchecked")
            Phrase<V> tk = (Phrase<V>) declaredConstructor.newInstance(btc, bp, bc);
            for ( Field field : astClass.getDeclaredFields() ) {
                NepoComponent fieldAnno = field.getAnnotation(NepoComponent.class);
                if ( fieldAnno != null ) {
                    if ( fieldAnno.isFieldWithDefault().equals(NepoComponent.DEFAULT_FIELD_VALUE) ) {
                        List<Value> values = block.getValue();
                        Phrase<V> sub = extractValue(values, new ExprParam(fieldAnno.fieldName(), fieldAnno.fieldType()));
                        if ( field.getType().equals(Expr.class) ) {
                            assignToPublicFinalField(astClass, tk, field, AbstractJaxb2Ast.convertPhraseToExpr(sub));
                        } else {
                            throw new DbcException(
                                "type of " + field.getType().getSimpleName() + " in AST class " + astClass.getSimpleName() + " not supported");
                        }
                    } else {
                        List<de.fhg.iais.roberta.blockly.generated.Field> xmlFields = block.getField();
                        String fieldValue = null;
                        for ( de.fhg.iais.roberta.blockly.generated.Field xmlField : xmlFields ) {
                            if ( xmlField.getName().equals(fieldAnno.fieldName()) ) {
                                fieldValue = xmlField.getValue();
                            }
                        }
                        if ( fieldValue == null ) {
                            fieldValue = fieldAnno.isFieldWithDefault();
                        }
                        assignToPublicFinalField(astClass, tk, field, fieldValue);
                    }
                }
            }
            return tk;
        } catch ( NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException | IllegalArgumentException e ) {
            throw new DbcException("constructor in AST class " + astClass.getSimpleName() + " not found or invalid", e);
        }
    }

    private void assignToPublicFinalField(Class<?> astClass, Phrase<V> tk, Field field, Object sub) {
        try {
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(tk, sub);
            modifiersField.setInt(field, field.getModifiers() & Modifier.FINAL);
            modifiersField.setAccessible(false);
            field.setAccessible(false);
        } catch ( IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e ) {
            throw new DbcException("field " + field.getName() + " in AST class " + astClass.getSimpleName() + " could not be assigned to", e);
        }
    }
}
