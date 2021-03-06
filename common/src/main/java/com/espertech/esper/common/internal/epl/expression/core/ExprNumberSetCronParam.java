/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.type.CronOperatorEnum;
import com.espertech.esper.common.internal.type.CronParameter;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Expression for a parameter within a crontab.
 * <p>
 * May have one subnode depending on the cron parameter type.
 */
public class ExprNumberSetCronParam extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private static final Logger log = LoggerFactory.getLogger(ExprNumberSetCronParam.class);

    public final static String METHOD_HANDLENUMBERSETCRONPARAMNULLVALUE = "handleNumberSetCronParamNullValue";

    private final CronOperatorEnum cronOperator;
    private transient ExprEvaluator evaluator;

    /**
     * Ctor.
     *
     * @param cronOperator type of cron parameter
     */
    public ExprNumberSetCronParam(CronOperatorEnum cronOperator) {
        this.cronOperator = cronOperator;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    /**
     * Returns the cron parameter type.
     *
     * @return type of cron parameter
     */
    public CronOperatorEnum getCronOperator() {
        return cronOperator;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public Class getEvaluationType() {
        return CronParameter.class;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        if (this.getChildNodes().length != 0) {
            this.getChildNodes()[0].toEPL(writer, getPrecedence(), flags);
            writer.append(" ");
        }
        writer.append(cronOperator.getSyntax());
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public ExprForgeConstantType getForgeConstantType() {
        if (this.getChildNodes().length == 0) {
            return ExprForgeConstantType.COMPILETIMECONST;
        }
        return this.getChildNodes()[0].getForge().getForgeConstantType();
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprNumberSetCronParam)) {
            return false;
        }
        ExprNumberSetCronParam other = (ExprNumberSetCronParam) node;
        return other.cronOperator.equals(this.cronOperator);
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length == 0) {
            return null;
        }
        ExprForge forge = this.getChildNodes()[0].getForge();
        if (!(JavaClassHelper.isNumericNonFP(forge.getEvaluationType()))) {
            throw new ExprValidationException("Frequency operator requires an integer-type parameter");
        }
        evaluator = forge.getExprEvaluator();
        return null;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (this.getChildNodes().length == 0) {
            return new CronParameter(cronOperator, null);
        }
        Object value = evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (value == null) {
            handleNumberSetCronParamNullValue();
            return new CronParameter(cronOperator, null);
        } else {
            int intValue = ((Number) value).intValue();
            return new CronParameter(cronOperator, intValue);
        }
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression enumValue = enumValue(CronOperatorEnum.class, cronOperator.name());
        CodegenExpression defaultValue = newInstance(CronParameter.class, enumValue, constantNull());
        if (this.getChildNodes().length == 0) {
            return defaultValue;
        }
        ExprForge forge = this.getChildNodes()[0].getForge();
        Class evaluationType = forge.getEvaluationType();
        CodegenMethod methodNode = codegenMethodScope.makeChild(CronParameter.class, ExprNumberSetCronParam.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(evaluationType, "value", forge.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope));
        if (!evaluationType.isPrimitive()) {
            block.ifRefNull("value")
                    .staticMethod(ExprNumberSetCronParam.class, METHOD_HANDLENUMBERSETCRONPARAMNULLVALUE)
                    .blockReturn(defaultValue);
        }
        block.methodReturn(newInstance(CronParameter.class, enumValue, SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("value"), evaluationType)));
        return localMethod(methodNode);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     */
    public static void handleNumberSetCronParamNullValue() {
        log.warn("Null value returned for cron parameter");
    }
}
