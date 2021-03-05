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
import de.fhg.iais.roberta.javaServer.restServices.all.controller.ProjectWorkflowRestController;
import de.fhg.iais.roberta.util.Pair;
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

import javax.xml.bind.JAXBException;

import static org.junit.Assert.fail;

public class CommonAstUnitTest {
    private static final Logger LOG = LoggerFactory.getLogger(CommonAstUnitTest.class);
    private static final String ROBOT_GROUP = "ev3";
    private static final String ROBOT_NAME_FOR_COMMON_TESTS = "ev3lejosv1";
    private static final String TARGET_DIR = "target/unitTests";
    private static final String RESOURCE_BASE_COMMON = "/crossCompilerTests/common/";
    private static final String RESOURCE_BASE_SPECIFIC = "/crossCompilerTests/robotSpecific/";
    private static final boolean SHOW_SUCCESS = true;
    private static final boolean STORE_DATA_INTO_FILES = false;
    private static final boolean STORE_DATA_INTO_FILES_ON_ERROR = true;

    private static final List<String> pluginDefines = new ArrayList<>();
    private static JSONObject progDeclsFromTestSpec;
    private static JSONObject robotsFromTestSpec;

    private int errorCount = 0;
    private int successCount = 0;

    @BeforeClass
    public static void setup() throws IOException {
        Path path = Paths.get(TARGET_DIR);
        Files.createDirectories(path);
        JSONObject testSpecification = Util.loadYAML("classpath:/crossCompilerTests/testSpec.yml");
        progDeclsFromTestSpec = testSpecification.getJSONObject("progs");
        robotsFromTestSpec = testSpecification.getJSONObject("robots");
    }

    @Test
    public void testCommonPartAsUnitTests() throws Exception {
        LOG.info("testing XML and AST consistency for " + progDeclsFromTestSpec.length() + " common programs");
        List<String> pluginDefines = new ArrayList<>(); // maybe used later to add properties
        IRobotFactory testFactory = Util.configureRobotPlugin(ROBOT_NAME_FOR_COMMON_TESTS, "", "", pluginDefines);
        String templateUnit = Util.readResourceContent(RESOURCE_BASE_COMMON + "/template/commonAstUnit.xml");
        JSONObject robotDeclFromTestSpec = robotsFromTestSpec.getJSONObject(ROBOT_NAME_FOR_COMMON_TESTS);
        String robotDir = robotDeclFromTestSpec.getString("template");
        String templateWithConfig = getTemplateWithConfigReplaced(robotDir, ROBOT_NAME_FOR_COMMON_TESTS);
        final String[] programNameArray = progDeclsFromTestSpec.keySet().toArray(new String[0]);
        Arrays.sort(programNameArray);
        nextProg: for ( String progName : programNameArray ) {
            LOG.info("processing program: " + progName);
            JSONObject progDeclFromTestSpec = progDeclsFromTestSpec.getJSONObject(progName);
            String generatedFinalXml = generateFinalProgram(templateWithConfig, progName, progDeclFromTestSpec);
            String generatedFragmentXml = generateFinalProgramFragment(templateUnit, progName, progDeclFromTestSpec);
            if ( STORE_DATA_INTO_FILES ) {
                storeDataIntoFiles(generatedFinalXml, "commonComplete", progName + "Full", "xml");
            }
            processProgramXml(progName, generatedFragmentXml, testFactory, "commonAst", "commonGenerated", "commonRegenerated");
        }
        LOG.info("succeeding tests: " + successCount);
        if ( errorCount > 0 ) {
            LOG.error("errors found: " + errorCount);
            Assert.fail("errors found: " + errorCount);
        }
    }

    @Test
    public void testRobotSpecificPartAsUnitTests() throws Exception {
        LOG.info("testing XML and AST consistency for robot specific programs");
        final String[] robotNameArray = robotsFromTestSpec.keySet().toArray(new String[0]);
        Arrays.sort(robotNameArray);
        for ( final String robotName : robotNameArray ) {
            LOG.info("*** processing robot: " + robotName);
            List<String> pluginDefines = new ArrayList<>(); // maybe used later to add properties
            IRobotFactory testFactory = Util.configureRobotPlugin(robotName, "", "", pluginDefines);
            JSONObject robot = robotsFromTestSpec.getJSONObject(robotName);
            final String robotDir = robot.getString("dir");
            final String resourceDirectory = RESOURCE_BASE_SPECIFIC + robotDir;
            de.fhg.iais.roberta.util.FileUtils.fileStreamOfResourceDirectory(resourceDirectory). //
                filter(f -> f.endsWith(".xml")).forEach(f -> extractProgramFragmentAndProcessProgramXml(f, robotName, resourceDirectory, testFactory));

        }
        LOG.info("succeeding tests: " + successCount);
        if ( errorCount > 0 ) {
            LOG.error("errors found: " + errorCount);
            Assert.fail("errors found: " + errorCount);
        }
    }

    private void extractProgramFragmentAndProcessProgramXml(
        String fileNameWithRobotSpecificTestProgram,
        String robotName,
        String directoryWithPrograms,
        IRobotFactory testFactory) //
    {
        int index = fileNameWithRobotSpecificTestProgram.lastIndexOf(".xml");
        Assert.assertTrue(index > 0);
        String progName = fileNameWithRobotSpecificTestProgram.substring(0, index);
        LOG.info("processing program: " + progName);
        String programFileName = directoryWithPrograms + "/" + fileNameWithRobotSpecificTestProgram;
        String exportXmlText = Util.readResourceContent(programFileName);
        Pair<String, String> progConfPair = ProjectWorkflowRestController.splitExportXML(exportXmlText);
        String programXml = progConfPair.getFirst();
        processProgramXml(progName, programXml, testFactory, robotName + "/specificAst", robotName + "/specificGenerated", robotName + "/specificRegenerated");
    }

    private void processProgramXml(
        String programName,
        String programXml,
        IRobotFactory testFactory,
        String directoryForAst,
        String directoryForGenerated,
        String directoryForRegenerated) //
    {
        if ( programName.equals("error") ) {
            LOG.info("ignoring program error");
            return;
        }
        BlockSet blockSet = null;
        try {
            blockSet = JaxbHelper.xml2BlockSet(programXml);
        } catch ( JAXBException e ) {
            Assert.fail("invalid program " + programName);
        }
        Assert.assertEquals("3.1", blockSet.getXmlversion());
        Jaxb2ProgramAst<Void> transformer = new Jaxb2ProgramAst<>(testFactory);
        ProgramAst<Void> generatedAst = transformer.blocks2Ast(blockSet);
        List<Phrase<Void>> blocks = generatedAst.getTree().get(0);
        StringBuilder sb = new StringBuilder();
        for ( int i = 2; i < blocks.size(); i++ ) {
            sb.append(blocks.get(i).toString()).append("\n");
        }
        if ( STORE_DATA_INTO_FILES ) {
            storeDataIntoFiles(sb.toString(), directoryForAst, programName, "ast");
        }
        // 1. check, that the regenerated XML is the same as the supplied XML
        Project.Builder builder = UnitTestHelper.setupWithProgramXML(testFactory, programXml);
        Project project = builder.build();
        String regeneratedProgramXml = project.getAnnotatedProgramAsXml();
        String diff = UnitTestHelper.runXmlUnit(programXml, regeneratedProgramXml);
        if ( STORE_DATA_INTO_FILES || diff != null ) {
            storeDataIntoFiles(programXml, directoryForGenerated, programName, "xml");
            storeDataIntoFiles(regeneratedProgramXml, directoryForRegenerated, programName, "xml");
        }
        if ( diff != null ) {
            LOG.error(diff);
            errorCount++;
        } else {
            successCount++;
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
        String template = Util.readResourceContent(RESOURCE_BASE_COMMON + "template/" + robotDir + ".xml");
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
            return Util.readResourceContent(RESOURCE_BASE_COMMON + directoryName + "/" + progNameWithXmlSuffix);
        } catch ( Exception e ) {
            // this happens, if no decl or fragment is available for the program given. This is legal.
            return null;
        }
    }

    public static void storeDataIntoFiles(String source, String directory, String programName, String suffix) {
        try {
            File sourceFile = new File(TARGET_DIR + "/" + directory + "/" + programName + "." + suffix);
            FileUtils.writeStringToFile(sourceFile, source, StandardCharsets.UTF_8.displayName());
        } catch ( Exception e ) {
            Assert.fail("Storing " + programName + " into directory " + TARGET_DIR + " failed");
        }
    }
}
