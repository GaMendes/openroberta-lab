package de.fhg.iais.roberta.syntax.expr;

import org.junit.Test;

import de.fhg.iais.roberta.syntax.CalliopeAstTest;
import de.fhg.iais.roberta.util.test.UnitTestHelper;

public class RgbColorTest extends CalliopeAstTest {

    @Test
    public void make_ByDefault_ReturnInstancesOfRgbColorClass() throws Exception {
        String expectedResult =
            "BlockAST[project=[[Location[x=163,y=37],MainTask[],DisplayTextAction[TEXT,RgbColor[R:NumConst[20],G:NumConst[25],B:NumConst[30],A:NumConst[30]]]]]]";
        UnitTestHelper.checkProgramAstEquality(testFactory, expectedResult, "/expr/create_color.xml");

    }

    @Test
    public void astToBlock_XMLtoJAXBtoASTtoXML_ReturnsSameXML() throws Exception {
        UnitTestHelper.checkProgramReverseTransformation(testFactory, "/expr/create_color.xml");
    }

}
