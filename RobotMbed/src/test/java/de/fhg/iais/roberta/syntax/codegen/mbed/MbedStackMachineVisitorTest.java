package de.fhg.iais.roberta.syntax.codegen.mbed;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import de.fhg.iais.roberta.bean.IProjectBean;
import de.fhg.iais.roberta.bean.UsedHardwareBean;
import de.fhg.iais.roberta.bean.UsedMethodBean;
import de.fhg.iais.roberta.visitor.validate.MbedValidatorAndCollectorVisitor;
import de.fhg.iais.roberta.worker.Calliope2017ValidatorAndCollectorWorker;
import org.junit.Test;

import de.fhg.iais.roberta.syntax.CalliopeAstTest;
import de.fhg.iais.roberta.util.Util;
import de.fhg.iais.roberta.util.test.UnitTestHelper;
import de.fhg.iais.roberta.worker.MbedStackMachineGeneratorWorker;
import de.fhg.iais.roberta.worker.MbedTwo2ThreeTransformerWorker;

public class MbedStackMachineVisitorTest extends CalliopeAstTest {

    @Test
    public void mbedDisplayTest() throws Exception {
        UnitTestHelper
            .checkWorkersWithConf(
                testFactory,
                configuration,
                Util.readResourceContent("/stack_machine/display.json"),
                "/stack_machine/display.xml",
                new Calliope2017ValidatorAndCollectorWorker(),
                new MbedStackMachineGeneratorWorker());
    }

    @Test
    public void mbedLightTest() throws Exception {
        UnitTestHelper
            .checkWorkersWithConf(
                testFactory,
                configuration,
                Util.readResourceContent("/stack_machine/light.json"),
                "/stack_machine/light.xml",
                new MbedTwo2ThreeTransformerWorker(),
                new Calliope2017ValidatorAndCollectorWorker(),
                new MbedStackMachineGeneratorWorker());
    }

    @Test
    public void mbedMoveTest() throws Exception {
        UnitTestHelper
            .checkWorkersWithConf(
                testFactory,
                configuration,
                Util.readResourceContent("/stack_machine/move.json"),
                "/stack_machine/move.xml",
                new MbedTwo2ThreeTransformerWorker(),
                new Calliope2017ValidatorAndCollectorWorker(),
                new MbedStackMachineGeneratorWorker());
    }

    @Test
    public void mbedSoundTest() throws Exception {
        UnitTestHelper
            .checkWorkersWithConf(
                testFactory,
                configuration,
                Util.readResourceContent("/stack_machine/sound.json"),
                "/stack_machine/sound.xml",
                new Calliope2017ValidatorAndCollectorWorker(),
                new MbedStackMachineGeneratorWorker());
    }

    @Test
    public void mbedPinTest() throws Exception {
        UnitTestHelper
            .checkWorkersWithConf(
                testFactory,
                configuration,
                Util.readResourceContent("/stack_machine/pin.json"),
                "/stack_machine/pin.xml",
                new MbedTwo2ThreeTransformerWorker(),
                new Calliope2017ValidatorAndCollectorWorker(),
                new MbedStackMachineGeneratorWorker());
    }

    @Test
    public void mbedSensorsTest() throws Exception {
        UnitTestHelper
            .checkWorkersWithConf(
                testFactory,
                configuration,
                Util.readResourceContent("/stack_machine/sensors.json"),
                "/stack_machine/sensors.xml",
                new MbedTwo2ThreeTransformerWorker(),
                new Calliope2017ValidatorAndCollectorWorker(),
                new MbedStackMachineGeneratorWorker());
    }

    private static ImmutableClassToInstanceMap<IProjectBean.IBuilder<?>> getCollectorMap() {
        ImmutableClassToInstanceMap<IProjectBean.IBuilder<?>> map = ImmutableClassToInstanceMap.of();
        map.put(UsedMethodBean.Builder.class, new UsedMethodBean.Builder());
        map.put(UsedHardwareBean.Builder.class, new UsedHardwareBean.Builder());
        return map;
    }
}
