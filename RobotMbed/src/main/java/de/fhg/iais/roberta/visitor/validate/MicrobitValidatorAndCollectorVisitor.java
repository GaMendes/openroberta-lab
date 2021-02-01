package de.fhg.iais.roberta.visitor.validate;

import com.google.common.collect.ClassToInstanceMap;
import de.fhg.iais.roberta.bean.IProjectBean;
import de.fhg.iais.roberta.components.ConfigurationAst;

public class MicrobitValidatorAndCollectorVisitor extends MbedValidatorAndCollectorVisitor {
    public MicrobitValidatorAndCollectorVisitor(
        ConfigurationAst brickConfiguration,
        ClassToInstanceMap<IProjectBean.IBuilder<?>> beanBuilders) {
        super(brickConfiguration, beanBuilders);
    }
}
