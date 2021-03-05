package de.fhg.iais.roberta.javaServer.integrationTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import de.fhg.iais.roberta.components.Project;
import de.fhg.iais.roberta.util.test.UnitTestHelper;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.iais.roberta.blockly.generated.BlockSet;
import de.fhg.iais.roberta.components.ProgramAst;
import de.fhg.iais.roberta.factory.IRobotFactory;
import de.fhg.iais.roberta.syntax.Phrase;
import de.fhg.iais.roberta.transformer.Jaxb2ProgramAst;
import de.fhg.iais.roberta.util.Util;
import de.fhg.iais.roberta.util.jaxb.JaxbHelper;
import org.xmlunit.diff.Diff;

public class CommonAstUnitTest {
    private static final Logger LOG = LoggerFactory.getLogger(CommonAstUnitTest.class);
    private static final List<String> pluginDefines = new ArrayList<>();
    private static final List<String> pluginDefinesNewConf = new ArrayList<>();
    protected static IRobotFactory testFactory;
    protected static IRobotFactory testFactoryNewConf;
    private static boolean showSuccess;
    private static JSONObject progDeclsFromTestSpec;
    private static JSONObject robotsFromTestSpec;

    private static final String ROBOT_GROUP = "ev3";
    private static final String ROBOT_NAME = "ev3lejosv1";
    private static final String TARGET_DIR = "target/snitTests";
    private static final String RESOURCE_BASE = "/crossCompilerTests/common/";

    @BeforeClass
    public static void setup() throws IOException {
        List<String> pluginDefines = new ArrayList<>(); // maybe used later to add properties
        List<String> pluginDefinesNewConf = new ArrayList<>(); // maybe used later to add properties
        String robotName = "nxt";
        testFactory = Util.configureRobotPlugin(robotName, "", "", pluginDefines);
        testFactoryNewConf = Util.configureRobotPlugin(robotName, "", "", pluginDefinesNewConf);

        Path path = Paths.get(TARGET_DIR);
        Files.createDirectories(path);

        JSONObject testSpecification = Util.loadYAML("classpath:/crossCompilerTests/testSpec.yml");
        showSuccess = testSpecification.getBoolean("showsuccess");
        progDeclsFromTestSpec = testSpecification.getJSONObject("progs");
        robotsFromTestSpec = testSpecification.getJSONObject("robots");
        LOG.info("testing XML and AST consistency for " + progDeclsFromTestSpec.length() + " programs");
    }

    @Test
    public void testCommonPartAsUnitTests() throws Exception {
        String templateUnit = Util.readResourceContent(RESOURCE_BASE + "/template/commonAstUnit.xml");
        JSONObject robotDeclFromTestSpec = robotsFromTestSpec.getJSONObject(ROBOT_NAME);
        String robotDir = robotDeclFromTestSpec.getString("template");
        String templateWithConfig = getTemplateWithConfigReplaced(robotDir, ROBOT_NAME);
        final String[] programNameArray = progDeclsFromTestSpec.keySet().toArray(new String[0]);
        Arrays.sort(programNameArray);
        int errorCount = 0;
        nextProg: for ( String progName : programNameArray ) {
            LOG.info("processing program: " + progName);
            JSONObject progDeclFromTestSpec = progDeclsFromTestSpec.getJSONObject(progName);
//            for unit tests, run all tests. Crashing crosscompiler etc. is no excuse!
//            JSONObject exclude = progDeclFromTestSpec.optJSONObject("exclude");
//            if ( exclude != null ) {
//                for ( String excludeRobot : exclude.keySet() ) {
//                    if ( excludeRobot.equals(ROBOT_GROUP) || excludeRobot.equals("ALL") ) {
//                        LOG.info("########## for " + ROBOT_GROUP + " prog " + progName + " is excluded. Reason: " + exclude.getString(excludeRobot));
//                        continue nextProg;
//                    }
//                }
//            }
            String generatedFinalXml = generateFinalProgram(templateWithConfig, progName, progDeclFromTestSpec);
            String generatedFragmentXml = generateFinalProgramFragment(templateUnit, progName, progDeclFromTestSpec);
            storeGenerated(generatedFinalXml, "complete", progName + "Full", "xml");
            storeGenerated(generatedFragmentXml, "fragment", progName, "xml");
            BlockSet blockSet = JaxbHelper.xml2BlockSet(generatedFragmentXml);
            Assert.assertEquals("3.1", blockSet.getXmlversion());
            Jaxb2ProgramAst<Void> transformer = new Jaxb2ProgramAst<>(testFactory);
            ProgramAst<Void> generatedAst = transformer.blocks2Ast(blockSet);
            List<Phrase<Void>> blocks = generatedAst.getTree().get(0);
            StringBuilder sb = new StringBuilder();
            for ( int i = 2; i < blocks.size(); i++ ) {
                sb.append(blocks.get(i).toString()).append("\n");
            }
            storeGenerated(sb.toString(), "ast", progName, "ast");

            // 1. check: the regenerated XML is the same as the supplied XML
            Project.Builder builder = UnitTestHelper.setupWithProgramXML(testFactory, generatedFragmentXml);
            Project project = builder.build();
            String annotatedProgramXml = project.getAnnotatedProgramAsXml();
            storeGenerated(annotatedProgramXml, "annotatedFragment", progName, "xml");
            String diff = UnitTestHelper.runXmlUnit(generatedFragmentXml, annotatedProgramXml);
            if (diff != null) {
                LOG.error(diff);
                errorCount++;
            }
        }
        if (errorCount > 0) {
            LOG.error("errors found: " + errorCount);
            Assert.fail("errors found: " + errorCount);
        }
    }

    private static String generateFinalProgramFragment(String template, String progName, JSONObject progDeclFromTestSpec) {
        String progSource = read("prog", progName + ".xml");
        Assert.assertNotNull(progSource, "program not found: " + progName);
        template = template.replaceAll("\\[\\[prog\\]\\]", progSource);
        String progFragmentName = progDeclFromTestSpec.optString("fragment");
        String progFragment = progFragmentName == null ? "" : read("fragment", progFragmentName + ".xml");
        template = template.replaceAll("\\[\\[fragment\\]\\]", progFragment == null ? "" : progFragment);
        String declName = progDeclFromTestSpec.optString("decl");
        Assert.assertNotNull(declName, "decl for program not found: " + progName);
        String decl = read("decl", declName + ".xml");
        template = template.replaceAll("\\[\\[decl\\]\\]", decl);
        return template;
    }

    private static String getTemplateWithConfigReplaced(String robotDir, String robotName) {
        String template = Util.readResourceContent(RESOURCE_BASE + "template/" + robotDir + ".xml");
        Properties robotProperties = Util.loadProperties("classpath:/" + robotName + ".properties");
        String defaultConfigurationURI = robotProperties.getProperty("robot.configuration.default");
        String defaultConfig = Util.readResourceContent(defaultConfigurationURI);
        final String templateWithConfig = template.replaceAll("\\[\\[conf\\]\\]", defaultConfig);
        return templateWithConfig;
    }

    private static String generateFinalProgram(String template, String progName, JSONObject progDeclFromTestSpec) {
        String progSource = read("prog", progName + ".xml");
        Assert.assertNotNull(progSource, "program not found: " + progName);
        template = template.replaceAll("\\[\\[prog\\]\\]", progSource);
        String progFragmentName = progDeclFromTestSpec.optString("fragment");
        String progFragment = progFragmentName == null ? "" : read("fragment", progFragmentName + ".xml");
        template = template.replaceAll("\\[\\[fragment\\]\\]", progFragment == null ? "" : progFragment);
        String declName = progDeclFromTestSpec.optString("decl");
        Assert.assertNotNull(declName, "decl for program not found: " + progName);
        String decl = read("decl", declName + ".xml");
        template = template.replaceAll("\\[\\[decl\\]\\]", decl);
        return template;
    }

    private static String read(String directoryName, String progNameWithXmlSuffix) {
        try {
            return Util.readResourceContent(RESOURCE_BASE + directoryName + "/" + progNameWithXmlSuffix);
        } catch ( Exception e ) {
            // this happens, if no decl or fragment is available for the program given. This is legal.
            return null;
        }
    }

    public static void storeGenerated(String source, String directory, String programName, String suffix) {
        try {
            File sourceFile = new File(TARGET_DIR + "/" + directory + "/" + programName + "." + suffix);
            FileUtils.writeStringToFile(sourceFile, source, StandardCharsets.UTF_8.displayName());
        } catch ( Exception e ) {
            Assert.fail("Storing " + programName + " into directory " + TARGET_DIR + " failed");
        }
    }
}
