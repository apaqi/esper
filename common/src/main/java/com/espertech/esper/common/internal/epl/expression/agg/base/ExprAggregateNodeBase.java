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
package com.espertech.esper.common.internal.epl.expression.agg.base;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldName;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationResultFuture;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

/**
 * Base expression node that represents an aggregation function such as 'sum' or 'count'.
 * <p>
 * In terms of validation each concrete aggregation node must implement it's own validation.
 * <p>
 * In terms of evaluation this base class will ask the assigned {@link AggregationResultFuture} for the current state,
 * using a column number assigned to the node.
 * <p>
 * Concrete subclasses must supply an aggregation state prototype node that reflects
 * each group's (there may be group-by critera) current aggregation state.
 */
public abstract class ExprAggregateNodeBase extends ExprNodeBase implements ExprEvaluator, ExprAggregateNode, ExprForgeInstrumentable {

    protected int column = -1;
    private AggregationForgeFactory aggregationForgeFactory;
    protected ExprAggregateLocalGroupByDesc optionalAggregateLocalGroupByDesc;
    protected ExprNode optionalFilter;
    protected ExprNode[] positionalParams;
    protected CodegenFieldName aggregationResultFutureMemberName;

    /**
     * Indicator for whether the aggregation is distinct - i.e. only unique values are considered.
     */
    protected boolean isDistinct;

    /**
     * Returns the aggregation function name for representation in a generate expression string.
     *
     * @return aggregation function name
     */
    public abstract String getAggregationFunctionName();

    protected abstract boolean isFilterExpressionAsLastParameter();

    /**
     * Return true if a expression aggregate node semantically equals the current node, or false if not.
     * <p>For use by the equalsNode implementation which compares the distinct flag.
     *
     * @param node to compare to
     * @return true if semantically equal, or false if not equals
     */
    protected abstract boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node);

    /**
     * Gives the aggregation node a chance to validate the sub-expression types.
     *
     * @param validationContext validation information
     * @return aggregation function factory to use
     * @throws ExprValidationException when expression validation failed
     */
    protected abstract AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext)
            throws ExprValidationException;

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    /**
     * Ctor.
     *
     * @param distinct - sets the flag indicatating whether only unique values should be aggregated
     */
    protected ExprAggregateNodeBase(boolean distinct) {
        isDistinct = distinct;
    }

    public ExprNode[] getPositionalParams() {
        return positionalParams;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public boolean isConstantResult() {
        return false;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        validatePositionals(validationContext);
        aggregationForgeFactory = validateAggregationChild(validationContext);
        if (!validationContext.isAggregationFutureNameAlreadySet()) {
            aggregationResultFutureMemberName = validationContext.getMemberNames().aggregationResultFutureRef();
        } else {
            if (aggregationResultFutureMemberName == null) {
                throw new ExprValidationException("Aggregation future not set");
            }
        }
        return null;
    }

    public void validatePositionals(ExprValidationContext validationContext) throws ExprValidationException {
        ExprAggregateNodeParamDesc paramDesc = ExprAggregateNodeUtil.getValidatePositionalParams(this.getChildNodes(), true);
        if (validationContext.getStatementRawInfo().getStatementType() == StatementType.CREATE_TABLE &&
                (paramDesc.getOptLocalGroupBy() != null || paramDesc.getOptionalFilter() != null)) {
            throw new ExprValidationException("The 'group_by' and 'filter' parameter is not allowed in create-table statements");
        }
        this.optionalAggregateLocalGroupByDesc = paramDesc.getOptLocalGroupBy();
        this.optionalFilter = paramDesc.getOptionalFilter();
        if (optionalAggregateLocalGroupByDesc != null) {
            ExprNodeUtilityValidate.validateNoSpecialsGroupByExpressions(optionalAggregateLocalGroupByDesc.getPartitionExpressions());
        }
        if (optionalFilter != null) {
            ExprNodeUtilityValidate.validateNoSpecialsGroupByExpressions(new ExprNode[]{optionalFilter});
        }
        if (optionalFilter != null && isFilterExpressionAsLastParameter()) {
            if (paramDesc.getPositionalParams().length > 1) {
                throw new ExprValidationException("Only a single filter expression can be provided");
            }
            positionalParams = ExprNodeUtilityMake.addExpression(paramDesc.getPositionalParams(), optionalFilter);
        } else {
            positionalParams = paramDesc.getPositionalParams();
        }
    }

    /**
     * Returns the aggregation state factory for use in grouping aggregation states per group-by keys.
     *
     * @return prototype aggregation state as a factory for aggregation states per group-by key value
     */
    public AggregationForgeFactory getFactory() {
        if (aggregationForgeFactory == null) {
            throw new IllegalStateException("Aggregation method has not been set");
        }
        return aggregationForgeFactory;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public final Object evaluate(EventBean[] events, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression future = getAggFuture(codegenClassScope);
        CodegenExpression eval = exprDotMethod(future, "getValue", constant(column), exprDotMethod(exprSymbol.getAddExprEvalCtx(parent), "getAgentInstanceId"), exprSymbol.getAddEPS(parent), exprSymbol.getAddIsNewData(parent), exprSymbol.getAddExprEvalCtx(parent));
        if (requiredType == Object.class) {
            return eval;
        }
        return CodegenLegoCast.castSafeFromObjectType(getEvaluationType(), eval);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprAggValue", requiredType, parent, exprSymbol, codegenClassScope).build();
    }

    public Class getEvaluationType() {
        if (aggregationForgeFactory == null) {
            throw new IllegalStateException("Aggregation method has not been set");
        }
        return aggregationForgeFactory.getResultType();
    }

    public ExprForge getForge() {
        return this;
    }

    /**
     * Returns true if the aggregation node is only aggregatig distinct values, or false if
     * aggregating all values.
     *
     * @return true if 'distinct' keyword was given, false if not
     */
    public boolean isDistinct() {
        return isDistinct;
    }

    public final boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprAggregateNode)) {
            return false;
        }

        ExprAggregateNode other = (ExprAggregateNode) node;

        if (other.isDistinct() != this.isDistinct) {
            return false;
        }

        return this.equalsNodeAggregateMethodOnly(other);
    }

    public int getColumn() {
        return column;
    }

    protected final Class validateNumericChildAllowFilter(boolean hasFilter)
            throws ExprValidationException {
        if (positionalParams.length == 0 || positionalParams.length > 2) {
            throw makeExceptionExpectedParamNum(1, 2);
        }

        // validate child expression (filter expression is actually always the first expression)
        ExprNode child = positionalParams[0];
        if (hasFilter) {
            validateFilter(positionalParams[1]);
        }

        Class childType = child.getForge().getEvaluationType();
        if (!JavaClassHelper.isNumeric(childType)) {
            throw new ExprValidationException("Implicit conversion from datatype '" +
                    (childType == null ? "null" : childType.getSimpleName()) +
                    "' to numeric is not allowed for aggregation function '" + getAggregationFunctionName() + "'");
        }

        return childType;
    }

    protected ExprValidationException makeExceptionExpectedParamNum(int lower, int upper) {
        String message = "The '" + getAggregationFunctionName() + "' function expects ";
        if (lower == 0 && upper == 0) {
            message += "no parameters";
        } else if (lower == upper) {
            message += lower + " parameters";
        } else {
            message += "at least " + lower + " and up to " + upper + " parameters";
        }
        return new ExprValidationException(message);
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        writer.append(getAggregationFunctionName());
        writer.append('(');

        if (isDistinct) {
            writer.append("distinct ");
        }

        if (this.getChildNodes().length > 0) {
            this.getChildNodes()[0].toEPL(writer, getPrecedence(), flags);

            String delimiter = ",";
            for (int i = 1; i < this.getChildNodes().length; i++) {
                writer.write(delimiter);
                delimiter = ",";
                this.getChildNodes()[i].toEPL(writer, getPrecedence(), flags);
            }
        } else {
            if (isExprTextWildcardWhenNoParams()) {
                writer.append('*');
            }
        }

        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.MINIMUM;
    }

    public void validateFilter(ExprNode filterEvaluator) throws ExprValidationException {
        if (JavaClassHelper.getBoxedType(filterEvaluator.getForge().getEvaluationType()) != Boolean.class) {
            throw new ExprValidationException("Invalid filter expression parameter to the aggregation function '" +
                    getAggregationFunctionName() +
                    "' is expected to return a boolean value but returns " + JavaClassHelper.getClassNameFullyQualPretty(filterEvaluator.getForge().getEvaluationType()));
        }
    }

    public ExprAggregateLocalGroupByDesc getOptionalLocalGroupBy() {
        return optionalAggregateLocalGroupByDesc;
    }

    public ExprNode getOptionalFilter() {
        return optionalFilter;
    }

    protected boolean isExprTextWildcardWhenNoParams() {
        return true;
    }

    public CodegenExpression getAggFuture(CodegenClassScope codegenClassScope) {
        return codegenClassScope.getPackageScope().addOrGetFieldWellKnown(aggregationResultFutureMemberName, AggregationResultFuture.class);
    }
}
