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
package com.espertech.esper.common.internal.avro.selectexprrep;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectExprProcessorEvalByGetterFragmentAvro implements ExprEvaluator, ExprForge, ExprNodeRenderable {
    private final int streamNum;
    private final EventPropertyGetterSPI getter;
    private final Class returnType;

    public SelectExprProcessorEvalByGetterFragmentAvro(int streamNum, EventPropertyGetterSPI getter, Class returnType) {
        this.streamNum = streamNum;
        this.getter = getter;
        this.returnType = returnType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean streamEvent = eventsPerStream[streamNum];
        if (streamEvent == null) {
            return null;
        }
        return getter.get(streamEvent);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(returnType, this.getClass(), codegenClassScope);

        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        methodNode.getBlock()
                .declareVar(EventBean.class, "streamEvent", arrayAtIndex(refEPS, constant(streamNum)))
                .ifRefNullReturnNull("streamEvent")
                .methodReturn(CodegenLegoCast.castSafeFromObjectType(returnType, getter.eventBeanGetCodegen(ref("streamEvent"), methodNode, codegenClassScope)));
        return localMethod(methodNode);
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence, ExprNodeRenderableFlags flags) {
        writer.append(this.getClass().getSimpleName());
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }
}
