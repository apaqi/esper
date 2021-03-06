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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.takewhile;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.*;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.*;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.util.function.Function;

public class ExprDotForgeTakeWhileAndLast extends ExprDotForgeLambdaThreeForm {

    protected EPType initAndNoParamsReturnType(EventType inputEventType, Class collectionComponentType) {
        throw new IllegalStateException();
    }

    protected ThreeFormNoParamFactory.ForgeFunction noParamsForge(EnumMethodEnum enumMethod, EPType type, StatementCompileTimeServices services) {
        throw new IllegalStateException();
    }

    protected Function<ExprDotEvalParamLambda, EPType> initAndSingleParamReturnType(EventType inputEventType, Class collectionComponentType) {
        return lambda -> {
            if (inputEventType != null) {
                return EPTypeHelper.collectionOfEvents(inputEventType);
            }
            return EPTypeHelper.collectionOfSingleValue(collectionComponentType);
        };
    }

    protected ThreeFormEventPlainFactory.ForgeFunction singleParamEventPlain(EnumMethodEnum enumMethod) {
        return (lambda, typeInfo, services) -> enumMethod == EnumMethodEnum.TAKEWHILELAST ? new EnumTakeWhileLastEvent(lambda) : new EnumTakeWhileEvent(lambda);
    }

    protected ThreeFormEventPlusFactory.ForgeFunction singleParamEventPlus(EnumMethodEnum enumMethod) {
        return (lambda, fieldType, numParameters, typeInfo, services) ->
            enumMethod == EnumMethodEnum.TAKEWHILELAST ? new EnumTakeWhileLastEventPlus(lambda, fieldType, numParameters) : new EnumTakeWhileEventPlus(lambda, fieldType, numParameters);
    }

    protected ThreeFormScalarFactory.ForgeFunction singleParamScalar(EnumMethodEnum enumMethod) {
        return (lambda, eventType, numParams, typeInfo, services) ->
            enumMethod == EnumMethodEnum.TAKEWHILELAST ? new EnumTakeWhileLastScalar(lambda, eventType, numParams) : new EnumTakeWhileScalar(lambda, eventType, numParams);
    }
}
