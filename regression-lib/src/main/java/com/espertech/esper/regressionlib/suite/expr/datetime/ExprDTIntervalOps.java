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
package com.espertech.esper.regressionlib.suite.expr.datetime;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;
import com.espertech.esper.regressionlib.support.bean.SupportTimeStartEndA;
import com.espertech.esper.regressionlib.support.bean.SupportTimeStartEndB;
import com.espertech.esper.regressionlib.support.schedule.SupportDateTimeFieldType;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExprDTIntervalOps {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprDTIntervalCalendarOps());
        executions.add(new ExprDTIntervalInvalid());
        executions.add(new ExprDTIntervalBeforeInSelectClause());
        executions.add(new ExprDTIntervalBeforeWhereClauseWithBean());
        executions.add(new ExprDTIntervalBeforeWhereClause());
        executions.add(new ExprDTIntervalAfterWhereClause());
        executions.add(new ExprDTIntervalCoincidesWhereClause());
        executions.add(new ExprDTIntervalDuringWhereClause());
        executions.add(new ExprDTIntervalFinishesWhereClause());
        executions.add(new ExprDTIntervalFinishedByWhereClause());
        executions.add(new ExprDTIntervalIncludesByWhereClause());
        executions.add(new ExprDTIntervalMeetsWhereClause());
        executions.add(new ExprDTIntervalMetByWhereClause());
        executions.add(new ExprDTIntervalOverlapsWhereClause());
        executions.add(new ExprDTIntervalOverlappedByWhereClause());
        executions.add(new ExprDTIntervalStartsWhereClause());
        executions.add(new ExprDTIntervalStartedByWhereClause());
        executions.add(new ExprDTIntervalPointInTimeWCalendarOps());
        executions.add(new ExprDTIntervalBeforeWVariable());
        executions.add(new ExprDTIntervalTimePeriodWYearNonConst());
        return executions;
    }

    private static class ExprDTIntervalBeforeWVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create variable int somenumber = 1;\n" +
                "@name('s0') select longdate.before(longdate, somenumber) as c0 from SupportDateTime;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(SupportDateTime.make("2002-05-30T09:00:00.000"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{false});

            env.undeployAll();
        }
    }

    private static class ExprDTIntervalTimePeriodWYearNonConst implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable int somenumber = 1", path);

            String epl = "@name('s0') select " +
                "longdate.before(longdate, somenumber years) as c0," +
                "longdate.before(longdate, somenumber month) as c1, " +
                "longdate.before(longdate, somenumber weeks) as c2, " +
                "longdate.before(longdate, somenumber days) as c3, " +
                "longdate.before(longdate, somenumber hours) as c4, " +
                "longdate.before(longdate, somenumber minutes) as c5, " +
                "longdate.before(longdate, somenumber seconds) as c6, " +
                "longdate.before(longdate, somenumber milliseconds) as c7, " +
                "longdate.before(longdate, somenumber microseconds) as c8 " +
                " from SupportDateTime";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(SupportDateTime.make("2002-05-30T09:00:00.000"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{false});

            env.undeployAll();
        }
    }

    private static class ExprDTIntervalPointInTimeWCalendarOps implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2,c3,c4".split(",");
            String epl = "@name('s0') select " +
                "longdate.set('month', 1).before(longPrimitive) as c0, " +
                "utildate.set('month', 1).before(longPrimitive) as c1," +
                "caldate.set('month', 1).before(longPrimitive) as c2," +
                "localdate.set('month', 1).before(longPrimitive) as c3," +
                "zoneddate.set('month', 1).before(longPrimitive) as c4 " +
                "from SupportDateTime unidirectional, SupportBean#lastevent";
            env.compileDeploy(epl).addListener("s0");

            SupportBean bean = new SupportBean();
            bean.setLongPrimitive(DateTime.parseDefaultMSec("2002-05-30T09:00:00.000"));
            env.sendEventBean(bean);

            env.sendEventBean(SupportDateTime.make("2002-05-30T09:00:00.000"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, true, true});

            env.sendEventBean(SupportDateTime.make("2003-05-30T08:00:00.000"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, false, false});

            env.undeployAll();
        }
    }

    private static class ExprDTIntervalCalendarOps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String seedTime = "2002-05-30T09:00:00.000"; // seed is time for B

            Object[][] expected = {
                {"2999-01-01T09:00:00.001", 0, true},       // sending in A
            };
            assertExpression(env, seedTime, 0, "a.withDate(2001, 1, 1).before(b)", expected, null);

            expected = new Object[][]{
                {"2999-01-01T10:00:00.001", 0, false},
                {"2999-01-01T08:00:00.001", 0, true},
            };
            assertExpression(env, seedTime, 0, "a.withDate(2001, 1, 1).before(b.withDate(2001, 1, 1))", expected, null);

            // Test end-timestamp preserved when using calendar op
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 2000, false},
            };
            assertExpression(env, seedTime, 0, "a.before(b)", expected, null);
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 2000, false},
            };
            assertExpression(env, seedTime, 0, "a.withTime(8, 59, 59, 0).before(b)", expected, null);

            // Test end-timestamp preserved when using calendar op
            expected = new Object[][]{
                {"2002-05-30T09:00:01.000", 0, false},
                {"2002-05-30T09:00:01.001", 0, true},
            };
            assertExpression(env, seedTime, 1000, "a.after(b)", expected, null);

            // NOT YET SUPPORTED (a documented limitation of datetime methods)
            // assertExpression(seedTime, 0, "a.after(b.withTime(9, 0, 0, 0))", expected, null);   // the "b.withTime(...) must retain the end-timestamp correctness (a documented limitation)
        }
    }

    private static class ExprDTIntervalInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // wrong 1st parameter - string
            tryInvalidCompile(env, "select a.before('x') from SupportTimeStartEndA as a",
                "Failed to validate select-clause expression 'a.before('x')': Failed to resolve enumeration method, date-time method or mapped property 'a.before('x')': For date-time method 'before' the first parameter expression returns 'class java.lang.String', however requires a Date, Calendar, Long-type return value or event (with timestamp)");

            // wrong 1st parameter - event not defined with timestamp expression
            tryInvalidCompile(env, "select a.before(b) from SupportTimeStartEndA#lastevent as a, SupportBean#lastevent as b",
                "Failed to validate select-clause expression 'a.before(b)': For date-time method 'before' the first parameter is event type 'SupportBean', however no timestamp property has been defined for this event type");

            // wrong 1st parameter - boolean
            tryInvalidCompile(env, "select a.before(true) from SupportTimeStartEndA#lastevent as a, SupportBean#lastevent as b",
                "Failed to validate select-clause expression 'a.before(true)': For date-time method 'before' the first parameter expression returns 'boolean', however requires a Date, Calendar, Long-type return value or event (with timestamp)");

            // wrong zero parameters
            tryInvalidCompile(env, "select a.before() from SupportTimeStartEndA#lastevent as a, SupportBean#lastevent as b",
                "Failed to validate select-clause expression 'a.before()': Parameters mismatch for date-time method 'before', the method has multiple footprints accepting an expression providing timestamp or timestamped-event, or an expression providing timestamp or timestamped-event and an expression providing interval start value, or an expression providing timestamp or timestamped-event and an expression providing interval start value and an expression providing interval finishes value, but receives no parameters");

            // wrong target
            tryInvalidCompile(env, "select theString.before(a) from SupportTimeStartEndA#lastevent as a, SupportBean#lastevent as b",
                "Failed to validate select-clause expression 'theString.before(a)': Date-time enumeration method 'before' requires either a Calendar, Date, long, LocalDateTime or ZonedDateTime value as input or events of an event type that declares a timestamp property but received java.lang.String");
            tryInvalidCompile(env, "select b.before(a) from SupportTimeStartEndA#lastevent as a, SupportBean#lastevent as b",
                "Failed to validate select-clause expression 'b.before(a)': Date-time enumeration method 'before' requires either a Calendar, Date, long, LocalDateTime or ZonedDateTime value as input or events of an event type that declares a timestamp property");
            tryInvalidCompile(env, "select a.get('month').before(a) from SupportTimeStartEndA#lastevent as a, SupportBean#lastevent as b",
                "Failed to validate select-clause expression 'a.get(\"month\").before(a)': Invalid input for date-time method 'before' ");

            // test before/after
            tryInvalidCompile(env, "select a.before(b, 'abc') from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.before(b,\"abc\")': Failed to validate date-time method 'before', expected a time-period expression or a numeric-type result for expression parameter 1 but received java.lang.String ");
            tryInvalidCompile(env, "select a.before(b, 1, 'def') from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.before(b,1,\"def\")': Failed to validate date-time method 'before', expected a time-period expression or a numeric-type result for expression parameter 2 but received java.lang.String ");
            tryInvalidCompile(env, "select a.before(b, 1, 2, 3) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.before(b,1,2,3)': Parameters mismatch for date-time method 'before', the method has multiple footprints accepting an expression providing timestamp or timestamped-event, or an expression providing timestamp or timestamped-event and an expression providing interval start value, or an expression providing timestamp or timestamped-event and an expression providing interval start value and an expression providing interval finishes value, but receives 4 expressions ");

            // test coincides
            tryInvalidCompile(env, "select a.coincides(b, 1, 2, 3) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.coincides(b,1,2,3)': Parameters mismatch for date-time method 'coincides', the method has multiple footprints accepting an expression providing timestamp or timestamped-event, or an expression providing timestamp or timestamped-event and an expression providing threshold for start and end value, or an expression providing timestamp or timestamped-event and an expression providing threshold for start value and an expression providing threshold for end value, but receives 4 expressions ");
            tryInvalidCompile(env, "select a.coincides(b, -1) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.coincides(b,-1)': The coincides date-time method does not allow negative start and end values ");

            // test during+interval
            tryInvalidCompile(env, "select a.during(b, 1, 2, 3) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.during(b,1,2,3)': Parameters mismatch for date-time method 'during', the method has multiple footprints accepting an expression providing timestamp or timestamped-event, or an expression providing timestamp or timestamped-event and an expression providing maximum distance interval both start and end, or an expression providing timestamp or timestamped-event and an expression providing minimum distance interval both start and end and an expression providing maximum distance interval both start and end, or an expression providing timestamp or timestamped-event and an expression providing minimum distance start and an expression providing maximum distance start and an expression providing minimum distance end and an expression providing maximum distance end, but receives 4 expressions ");

            // test finishes+finished-by
            tryInvalidCompile(env, "select a.finishes(b, 1, 2) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.finishes(b,1,2)': Parameters mismatch for date-time method 'finishes', the method has multiple footprints accepting an expression providing timestamp or timestamped-event, or an expression providing timestamp or timestamped-event and an expression providing maximum distance between end timestamps, but receives 3 expressions ");
            tryInvalidCompile(env, "select a.finishes(b, -1) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.finishes(b,-1)': The finishes date-time method does not allow negative threshold value ");
            tryInvalidCompile(env, "select a.finishedby(b, -1) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.finishedby(b,-1)': The finishedby date-time method does not allow negative threshold value ");

            // test meets+met-by
            tryInvalidCompile(env, "select a.meets(b, 1, 2) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.meets(b,1,2)': Parameters mismatch for date-time method 'meets', the method has multiple footprints accepting an expression providing timestamp or timestamped-event, or an expression providing timestamp or timestamped-event and an expression providing maximum distance between start and end timestamps, but receives 3 expressions ");
            tryInvalidCompile(env, "select a.meets(b, -1) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.meets(b,-1)': The meets date-time method does not allow negative threshold value ");
            tryInvalidCompile(env, "select a.metBy(b, -1) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.metBy(b,-1)': The metBy date-time method does not allow negative threshold value ");

            // test overlaps+overlapped-by
            tryInvalidCompile(env, "select a.overlaps(b, 1, 2, 3) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.overlaps(b,1,2,3)': Parameters mismatch for date-time method 'overlaps', the method has multiple footprints accepting an expression providing timestamp or timestamped-event, or an expression providing timestamp or timestamped-event and an expression providing maximum distance interval both start and end, or an expression providing timestamp or timestamped-event and an expression providing minimum distance interval both start and end and an expression providing maximum distance interval both start and end, but receives 4 expressions ");

            // test start/startedby
            tryInvalidCompile(env, "select a.starts(b, 1, 2, 3) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.starts(b,1,2,3)': Parameters mismatch for date-time method 'starts', the method has multiple footprints accepting an expression providing timestamp or timestamped-event, or an expression providing timestamp or timestamped-event and an expression providing maximum distance between start timestamps, but receives 4 expressions ");
            tryInvalidCompile(env, "select a.starts(b, -1) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.starts(b,-1)': The starts date-time method does not allow negative threshold value ");
            tryInvalidCompile(env, "select a.startedBy(b, -1) from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b",
                "Failed to validate select-clause expression 'a.startedBy(b,-1)': The startedBy date-time method does not allow negative threshold value ");
        }
    }

    private static class ExprDTIntervalBeforeInSelectClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') select " +
                "a.longdateStart.before(b.longdateStart) as c0," +
                "a.before(b) as c1 " +
                " from SupportTimeStartEndA#lastevent as a, " +
                "      SupportTimeStartEndB#lastevent as b";
            env.compileDeploy(epl).addListener("s0");
            LambdaAssertionUtil.assertTypesAllSame(env.statement("s0").getEventType(), fields, Boolean.class);

            env.sendEventBean(SupportTimeStartEndB.make("B1", "2002-05-30T09:00:00.000", 0));

            env.sendEventBean(SupportTimeStartEndA.make("A1", "2002-05-30T08:59:59.000", 0));
            EPAssertionUtil.assertPropsAllValuesSame(env.listener("s0").assertOneGetNewAndReset(), fields, true);

            env.sendEventBean(SupportTimeStartEndA.make("A2", "2002-05-30T08:59:59.950", 0));
            EPAssertionUtil.assertPropsAllValuesSame(env.listener("s0").assertOneGetNewAndReset(), fields, true);

            env.undeployAll();
        }
    }

    private static class ExprDTIntervalBeforeWhereClauseWithBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new BeforeValidator(1L, Long.MAX_VALUE);
            Object[][] expected = {
                {"2002-05-30T08:59:59.000", 0, true},
                {"2002-05-30T08:59:59.999", 0, true},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.001", 0, false},
            };

            String[] expressions = new String[]{
                "a.before(b)",
                "a.before(b, 1 millisecond)",
                "a.before(b, 1 millisecond, 1000000000L)",
                "a.longdateStart.before(b)",
                "a.utildateStart.before(b)",
                "a.caldateStart.before(b)",
                "a.before(b.longdateStart)",
                "a.before(b.utildateStart)",
                "a.before(b.caldateStart)",
                "a.longdateStart.before(b.longdateStart)",
                "a.longdateStart.before(b.longdateStart)",
                "a.utildateStart.before(b.utildateStart)",
                "a.caldateStart.before(b.caldateStart)",
                "a.utildateStart.before(b.caldateStart)",
                "a.utildateStart.before(b.longdateStart)",
                "a.caldateStart.before(b.utildateStart)",
                "a.caldateStart.before(b.longdateStart)",
                "a.ldtStart.before(b.ldtStart)",
                "a.zdtStart.before(b.zdtStart)"
            };
            String seedTime = "2002-05-30T09:00:00.000";
            for (String expression : expressions) {
                assertExpressionBean(env, seedTime, 0, expression, expected, expectedValidator);
            }
        }
    }

    private static class ExprDTIntervalBeforeWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String seedTime;
            Object[][] expected;
            BeforeValidator expectedValidator;

            seedTime = "2002-05-30T09:00:00.000";
            expectedValidator = new BeforeValidator(1L, Long.MAX_VALUE);
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 0, true},
                {"2002-05-30T08:59:59.000", 999, true},
                {"2002-05-30T08:59:59.000", 1000, false},
                {"2002-05-30T08:59:59.000", 1001, false},
                {"2002-05-30T08:59:59.999", 0, true},
                {"2002-05-30T08:59:59.999", 1, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.001", 0, false},
            };
            assertExpression(env, seedTime, 0, "a.before(b)", expected, expectedValidator);
            assertExpression(env, seedTime, 100000, "a.before(b)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 0, true},
                {"2002-05-30T08:59:59.899", 0, true},
                {"2002-05-30T08:59:59.900", 0, true},
                {"2002-05-30T08:59:59.901", 0, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.001", 0, false},
            };
            expectedValidator = new BeforeValidator(100L, Long.MAX_VALUE);
            assertExpression(env, seedTime, 0, "a.before(b, 100 milliseconds)", expected, expectedValidator);
            assertExpression(env, seedTime, 100000, "a.before(b, 100 milliseconds)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T08:59:59.499", 0, false},
                {"2002-05-30T08:59:59.499", 1, true},
                {"2002-05-30T08:59:59.500", 0, true},
                {"2002-05-30T08:59:59.500", 1, true},
                {"2002-05-30T08:59:59.500", 400, true},
                {"2002-05-30T08:59:59.500", 401, false},
                {"2002-05-30T08:59:59.899", 0, true},
                {"2002-05-30T08:59:59.899", 2, false},
                {"2002-05-30T08:59:59.900", 0, true},
                {"2002-05-30T08:59:59.900", 1, false},
                {"2002-05-30T08:59:59.901", 0, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.001", 0, false},
            };
            expectedValidator = new BeforeValidator(100L, 500L);
            assertExpression(env, seedTime, 0, "a.before(b, 100 milliseconds, 500 milliseconds)", expected, expectedValidator);
            assertExpression(env, seedTime, 100000, "a.before(b, 100 milliseconds, 500 milliseconds)", expected, expectedValidator);

            // test expression params
            setVStartEndVariables(env, 100, 500);
            assertExpression(env, seedTime, 0, "a.before(b, V_START milliseconds, V_END milliseconds)", expected, expectedValidator);

            setVStartEndVariables(env, 200, 800);
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T08:59:59.199", 0, false},
                {"2002-05-30T08:59:59.199", 1, true},
                {"2002-05-30T08:59:59.200", 0, true},
                {"2002-05-30T08:59:59.800", 0, true},
                {"2002-05-30T08:59:59.801", 0, false},
            };
            expectedValidator = new BeforeValidator(200L, 800L);
            assertExpression(env, seedTime, 0, "a.before(b, V_START milliseconds, V_END milliseconds)", expected, expectedValidator);

            // test negative and reversed max and min
            expected = new Object[][]{
                {"2002-05-30T08:59:59.500", 0, false},
                {"2002-05-30T09:00:00.990", 0, false},
                {"2002-05-30T09:00:00.100", 0, true},
                {"2002-05-30T09:00:00.500", 0, true},
                {"2002-05-30T09:00:00.501", 0, false},
            };
            expectedValidator = new BeforeValidator(-500L, -100L);
            assertExpression(env, seedTime, 0, "a.before(b, -100 milliseconds, -500 milliseconds)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.before(b, -500 milliseconds, -100 milliseconds)", expected, expectedValidator);

            // test month logic
            seedTime = "2002-03-01T09:00:00.000";
            expected = new Object[][]{
                {"2002-02-01T09:00:00.000", 0, true},
                {"2002-02-01T09:00:00.001", 0, false}
            };
            expectedValidator = new BeforeValidator(getMillisecForDays(28), Long.MAX_VALUE);
            assertExpression(env, seedTime, 100, "a.before(b, 1 month)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-01-01T08:59:59.999", 0, false},
                {"2002-01-01T09:00:00.000", 0, true},
                {"2002-01-11T09:00:00.000", 0, true},
                {"2002-02-01T09:00:00.000", 0, true},
                {"2002-02-01T09:00:00.001", 0, false}
            };
            expectedValidator = new BeforeValidator(getMillisecForDays(28), getMillisecForDays(28 + 31));
            assertExpression(env, seedTime, 100, "a.before(b, 1 month, 2 month)", expected, expectedValidator);
        }
    }

    private static class ExprDTIntervalAfterWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new AfterValidator(1L, Long.MAX_VALUE);
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.001", 0, true},
            };
            assertExpression(env, seedTime, 0, "a.after(b)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.after(b, 1 millisecond)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.after(b, 1 millisecond, 1000000000L)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.after(b, 1000000000L, 1 millisecond)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.startTS.after(b)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.after(b.startTS)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.001", 0, false},
                {"2002-05-30T09:00:00.002", 0, true},
            };
            assertExpression(env, seedTime, 1, "a.after(b)", expected, expectedValidator);
            assertExpression(env, seedTime, 1, "a.after(b, 1 millisecond, 1000000000L)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.099", 0, false},
                {"2002-05-30T09:00:00.100", 0, true},
                {"2002-05-30T09:00:00.101", 0, true},
            };
            expectedValidator = new AfterValidator(100L, Long.MAX_VALUE);
            assertExpression(env, seedTime, 0, "a.after(b, 100 milliseconds)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.after(b, 100 milliseconds, 1000000000L)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.099", 0, false},
                {"2002-05-30T09:00:00.100", 0, true},
                {"2002-05-30T09:00:00.500", 0, true},
                {"2002-05-30T09:00:00.501", 0, false},
            };
            expectedValidator = new AfterValidator(100L, 500L);
            assertExpression(env, seedTime, 0, "a.after(b, 100 milliseconds, 500 milliseconds)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.after(b, 100 milliseconds, 500 milliseconds)", expected, expectedValidator);

            // test expression params
            setVStartEndVariables(env, 100, 500);
            assertExpression(env, seedTime, 0, "a.after(b, V_START milliseconds, V_END milliseconds)", expected, expectedValidator);

            setVStartEndVariables(env, 200, 800);
            expected = new Object[][]{
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.199", 0, false},
                {"2002-05-30T09:00:00.200", 0, true},
                {"2002-05-30T09:00:00.800", 0, true},
                {"2002-05-30T09:00:00.801", 0, false},
            };
            expectedValidator = new AfterValidator(200L, 800L);
            assertExpression(env, seedTime, 0, "a.after(b, V_START milliseconds, V_END milliseconds)", expected, expectedValidator);

            // test negative distances
            expected = new Object[][]{
                {"2002-05-30T08:59:59.599", 0, false},
                {"2002-05-30T08:59:59.600", 0, true},
                {"2002-05-30T09:00:00.000", 0, true},
                {"2002-05-30T09:00:00.001", 0, false},
            };
            expectedValidator = new AfterValidator(-500L, -100L);
            assertExpression(env, seedTime, 100, "a.after(b, -100 milliseconds, -500 milliseconds)", expected, expectedValidator);
            assertExpression(env, seedTime, 100, "a.after(b, -500 milliseconds, -100 milliseconds)", expected, expectedValidator);

            // test month logic
            seedTime = "2002-02-01T09:00:00.000";
            expected = new Object[][]{
                {"2002-03-01T09:00:00.099", 0, false},
                {"2002-03-01T09:00:00.100", 0, true}
            };
            expectedValidator = new AfterValidator(getMillisecForDays(28), Long.MAX_VALUE);
            assertExpression(env, seedTime, 100, "a.after(b, 1 month)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-03-01T09:00:00.099", 0, false},
                {"2002-03-01T09:00:00.100", 0, true},
                {"2002-04-01T09:00:00.100", 0, true},
                {"2002-04-01T09:00:00.101", 0, false}
            };
            assertExpression(env, seedTime, 100, "a.after(b, 1 month, 2 month)", expected, null);
        }
    }

    private static class ExprDTIntervalCoincidesWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new CoincidesValidator();
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T09:00:00.000", 0, true},
                {"2002-05-30T09:00:00.001", 0, false},
            };
            assertExpression(env, seedTime, 0, "a.coincides(b)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.coincides(b, 0 millisecond)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.coincides(b, 0, 0)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.startTS.coincides(b)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.coincides(b.startTS)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T09:00:00.000", 1, true},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.001", 0, false},
                {"2002-05-30T09:00:00.001", 1, false},
            };
            assertExpression(env, seedTime, 1, "a.coincides(b)", expected, expectedValidator);
            assertExpression(env, seedTime, 1, "a.coincides(b, 0, 0)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T08:59:59.899", 0, false},
                {"2002-05-30T08:59:59.900", 0, true},
                {"2002-05-30T09:00:00.000", 0, true},
                {"2002-05-30T09:00:00.000", 50, true},
                {"2002-05-30T09:00:00.000", 100, true},
                {"2002-05-30T09:00:00.000", 101, false},
                {"2002-05-30T09:00:00.099", 0, true},
                {"2002-05-30T09:00:00.100", 0, true},
                {"2002-05-30T09:00:00.101", 0, false},
            };
            expectedValidator = new CoincidesValidator(100L);
            assertExpression(env, seedTime, 0, "a.coincides(b, 100 milliseconds)", expected, expectedValidator);
            assertExpression(env, seedTime, 0, "a.coincides(b, 100 milliseconds, 0.1 sec)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T08:59:59.799", 0, false},
                {"2002-05-30T08:59:59.800", 0, true},
                {"2002-05-30T09:00:00.000", 0, true},
                {"2002-05-30T09:00:00.099", 0, true},
                {"2002-05-30T09:00:00.100", 0, true},
                {"2002-05-30T09:00:00.200", 0, true},
                {"2002-05-30T09:00:00.201", 0, false},
            };
            expectedValidator = new CoincidesValidator(200L, 500L);
            assertExpression(env, seedTime, 0, "a.coincides(b, 200 milliseconds, 500 milliseconds)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T08:59:59.799", 0, false},
                {"2002-05-30T08:59:59.799", 200, false},
                {"2002-05-30T08:59:59.799", 201, false},
                {"2002-05-30T08:59:59.800", 0, false},
                {"2002-05-30T08:59:59.800", 199, false},
                {"2002-05-30T08:59:59.800", 200, true},
                {"2002-05-30T08:59:59.800", 300, true},
                {"2002-05-30T08:59:59.800", 301, false},
                {"2002-05-30T09:00:00.050", 0, true},
                {"2002-05-30T09:00:00.099", 0, true},
                {"2002-05-30T09:00:00.100", 0, true},
                {"2002-05-30T09:00:00.101", 0, false},
            };
            expectedValidator = new CoincidesValidator(200L, 50L);
            assertExpression(env, seedTime, 50, "a.coincides(b, 200 milliseconds, 50 milliseconds)", expected, expectedValidator);

            // test expression params
            setVStartEndVariables(env, 200, 50);
            assertExpression(env, seedTime, 50, "a.coincides(b, V_START milliseconds, V_END milliseconds)", expected, expectedValidator);

            setVStartEndVariables(env, 200, 70);
            expected = new Object[][]{
                {"2002-05-30T08:59:59.800", 0, false},
                {"2002-05-30T08:59:59.800", 179, false},
                {"2002-05-30T08:59:59.800", 180, true},
                {"2002-05-30T08:59:59.800", 200, true},
                {"2002-05-30T08:59:59.800", 320, true},
                {"2002-05-30T08:59:59.800", 321, false},
            };
            expectedValidator = new CoincidesValidator(200L, 70L);
            assertExpression(env, seedTime, 50, "a.coincides(b, V_START milliseconds, V_END milliseconds)", expected, expectedValidator);

            // test month logic
            seedTime = "2002-02-01T09:00:00.000";    // lasts to "2002-04-01T09:00:00.000" (28+31 days)
            expected = new Object[][]{
                {"2002-02-15T09:00:00.099", getMillisecForDays(28 + 14), true},
                {"2002-01-01T08:00:00.000", getMillisecForDays(28 + 30), false}
            };
            expectedValidator = new CoincidesValidator(getMillisecForDays(28));
            assertExpression(env, seedTime, getMillisecForDays(28 + 31), "a.coincides(b, 1 month)", expected, expectedValidator);
        }
    }

    private static class ExprDTIntervalDuringWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new DuringValidator();
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.001", 0, true},
                {"2002-05-30T09:00:00.001", 98, true},
                {"2002-05-30T09:00:00.001", 99, false},
                {"2002-05-30T09:00:00.099", 0, true},
                {"2002-05-30T09:00:00.099", 1, false},
                {"2002-05-30T09:00:00.100", 0, false},
            };
            assertExpression(env, seedTime, 100, "a.during(b)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.001", 0, false},
                {"2002-05-30T09:00:00.001", 1, false},
            };
            assertExpression(env, seedTime, 0, "a.during(b)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T09:00:00.001", 0, true},
                {"2002-05-30T09:00:00.001", 2000000, true},
            };
            assertExpression(env, seedTime, 100, "a.startTS.during(b)", expected, null);    // want to use null-validator here

            // test 1-parameter footprint
            expected = new Object[][]{
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 100, false},
                {"2002-05-30T09:00:00.001", 0, false},
                {"2002-05-30T09:00:00.001", 83, false},
                {"2002-05-30T09:00:00.001", 84, true},
                {"2002-05-30T09:00:00.001", 98, true},
                {"2002-05-30T09:00:00.001", 99, false},
                {"2002-05-30T09:00:00.015", 69, false},
                {"2002-05-30T09:00:00.015", 70, true},
                {"2002-05-30T09:00:00.015", 84, true},
                {"2002-05-30T09:00:00.015", 85, false},
                {"2002-05-30T09:00:00.016", 80, false},
                {"2002-05-30T09:00:00.099", 0, false},
            };
            expectedValidator = new DuringValidator(15L);
            assertExpression(env, seedTime, 100, "a.during(b, 15 milliseconds)", expected, expectedValidator);

            // test 2-parameter footprint
            expected = new Object[][]{
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 100, false},
                {"2002-05-30T09:00:00.001", 0, false},
                {"2002-05-30T09:00:00.001", 78, false},
                {"2002-05-30T09:00:00.001", 79, false},
                {"2002-05-30T09:00:00.004", 85, false},
                {"2002-05-30T09:00:00.005", 74, false},
                {"2002-05-30T09:00:00.005", 75, true},
                {"2002-05-30T09:00:00.005", 90, true},
                {"2002-05-30T09:00:00.005", 91, false},
                {"2002-05-30T09:00:00.006", 83, true},
                {"2002-05-30T09:00:00.020", 76, false},
                {"2002-05-30T09:00:00.020", 75, true},
                {"2002-05-30T09:00:00.020", 60, true},
                {"2002-05-30T09:00:00.020", 59, false},
                {"2002-05-30T09:00:00.021", 68, false},
                {"2002-05-30T09:00:00.099", 0, false},
            };
            expectedValidator = new DuringValidator(5L, 20L);
            assertExpression(env, seedTime, 100, "a.during(b, 5 milliseconds, 20 milliseconds)", expected, expectedValidator);

            // test 4-parameter footprint
            expected = new Object[][]{
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 100, false},
                {"2002-05-30T09:00:00.004", 85, false},
                {"2002-05-30T09:00:00.005", 64, false},
                {"2002-05-30T09:00:00.005", 65, true},
                {"2002-05-30T09:00:00.005", 85, true},
                {"2002-05-30T09:00:00.005", 86, false},
                {"2002-05-30T09:00:00.020", 49, false},
                {"2002-05-30T09:00:00.020", 50, true},
                {"2002-05-30T09:00:00.020", 70, true},
                {"2002-05-30T09:00:00.020", 71, false},
                {"2002-05-30T09:00:00.021", 55, false},
            };
            expectedValidator = new DuringValidator(5L, 20L, 10L, 30L);
            assertExpression(env, seedTime, 100, "a.during(b, 5 milliseconds, 20 milliseconds, 10 milliseconds, 30 milliseconds)", expected, expectedValidator);
        }
    }

    private static class ExprDTIntervalFinishesWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new FinishesValidator();
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.001", 0, false},
                {"2002-05-30T09:00:00.001", 98, false},
                {"2002-05-30T09:00:00.001", 99, true},
                {"2002-05-30T09:00:00.001", 100, false},
                {"2002-05-30T09:00:00.050", 50, true},
                {"2002-05-30T09:00:00.099", 0, false},
                {"2002-05-30T09:00:00.099", 1, true},
                {"2002-05-30T09:00:00.100", 0, true},
                {"2002-05-30T09:00:00.101", 0, false},
            };
            assertExpression(env, seedTime, 100, "a.finishes(b)", expected, expectedValidator);
            assertExpression(env, seedTime, 100, "a.finishes(b, 0)", expected, expectedValidator);
            assertExpression(env, seedTime, 100, "a.finishes(b, 0 milliseconds)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 99, false},
                {"2002-05-30T09:00:00.001", 93, false},
                {"2002-05-30T09:00:00.001", 94, true},
                {"2002-05-30T09:00:00.001", 100, true},
                {"2002-05-30T09:00:00.001", 104, true},
                {"2002-05-30T09:00:00.001", 105, false},
                {"2002-05-30T09:00:00.050", 50, true},
                {"2002-05-30T09:00:00.104", 0, true},
                {"2002-05-30T09:00:00.104", 1, true},
                {"2002-05-30T09:00:00.105", 0, true},
                {"2002-05-30T09:00:00.105", 1, false},
            };
            expectedValidator = new FinishesValidator(5L);
            assertExpression(env, seedTime, 100, "a.finishes(b, 5 milliseconds)", expected, expectedValidator);
        }
    }

    private static class ExprDTIntervalFinishedByWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new FinishedByValidator();
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T08:59:59.000", 1099, false},
                {"2002-05-30T08:59:59.000", 1100, true},
                {"2002-05-30T08:59:59.000", 1101, false},
                {"2002-05-30T08:59:59.999", 100, false},
                {"2002-05-30T08:59:59.999", 101, true},
                {"2002-05-30T08:59:59.999", 102, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 50, false},
                {"2002-05-30T09:00:00.000", 100, false},
            };
            assertExpression(env, seedTime, 100, "a.finishedBy(b)", expected, expectedValidator);
            assertExpression(env, seedTime, 100, "a.finishedBy(b, 0)", expected, expectedValidator);
            assertExpression(env, seedTime, 100, "a.finishedBy(b, 0 milliseconds)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T08:59:59.000", 1094, false},
                {"2002-05-30T08:59:59.000", 1095, true},
                {"2002-05-30T08:59:59.000", 1105, true},
                {"2002-05-30T08:59:59.000", 1106, false},
                {"2002-05-30T08:59:59.999", 95, false},
                {"2002-05-30T08:59:59.999", 96, true},
                {"2002-05-30T08:59:59.999", 106, true},
                {"2002-05-30T08:59:59.999", 107, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 95, false},
                {"2002-05-30T09:00:00.000", 100, false},
                {"2002-05-30T09:00:00.000", 105, false},
            };
            expectedValidator = new FinishedByValidator(5L);
            assertExpression(env, seedTime, 100, "a.finishedBy(b, 5 milliseconds)", expected, expectedValidator);
        }
    }

    private static class ExprDTIntervalIncludesByWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new IncludesValidator();
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T08:59:59.000", 1100, false},
                {"2002-05-30T08:59:59.000", 1101, true},
                {"2002-05-30T08:59:59.000", 3000, true},
                {"2002-05-30T08:59:59.999", 101, false},
                {"2002-05-30T08:59:59.999", 102, true},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 50, false},
                {"2002-05-30T09:00:00.000", 102, false},
            };
            assertExpression(env, seedTime, 100, "a.includes(b)", expected, expectedValidator);

            // test 1-parameter form
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T08:59:59.000", 1100, false},
                {"2002-05-30T08:59:59.000", 1105, false},
                {"2002-05-30T08:59:59.994", 106, false},
                {"2002-05-30T08:59:59.994", 110, false},
                {"2002-05-30T08:59:59.995", 105, false},
                {"2002-05-30T08:59:59.995", 106, true},
                {"2002-05-30T08:59:59.995", 110, true},
                {"2002-05-30T08:59:59.995", 111, false},
                {"2002-05-30T08:59:59.999", 101, false},
                {"2002-05-30T08:59:59.999", 102, true},
                {"2002-05-30T08:59:59.999", 106, true},
                {"2002-05-30T08:59:59.999", 107, false},
                {"2002-05-30T09:00:00.000", 105, false},
                {"2002-05-30T09:00:00.000", 106, false},
            };
            expectedValidator = new IncludesValidator(5L);
            assertExpression(env, seedTime, 100, "a.includes(b, 5 milliseconds)", expected, expectedValidator);

            // test 2-parameter form
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T08:59:59.000", 1100, false},
                {"2002-05-30T08:59:59.000", 1105, false},
                {"2002-05-30T08:59:59.979", 130, false},
                {"2002-05-30T08:59:59.980", 124, false},
                {"2002-05-30T08:59:59.980", 125, true},
                {"2002-05-30T08:59:59.980", 140, true},
                {"2002-05-30T08:59:59.980", 141, false},
                {"2002-05-30T08:59:59.995", 109, false},
                {"2002-05-30T08:59:59.995", 110, true},
                {"2002-05-30T08:59:59.995", 125, true},
                {"2002-05-30T08:59:59.995", 126, false},
                {"2002-05-30T08:59:59.996", 112, false},
            };
            expectedValidator = new IncludesValidator(5L, 20L);
            assertExpression(env, seedTime, 100, "a.includes(b, 5 milliseconds, 20 milliseconds)", expected, expectedValidator);

            // test 4-parameter form
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T08:59:59.000", 1100, false},
                {"2002-05-30T08:59:59.000", 1105, false},
                {"2002-05-30T08:59:59.979", 150, false},
                {"2002-05-30T08:59:59.980", 129, false},
                {"2002-05-30T08:59:59.980", 130, true},
                {"2002-05-30T08:59:59.980", 150, true},
                {"2002-05-30T08:59:59.980", 151, false},
                {"2002-05-30T08:59:59.995", 114, false},
                {"2002-05-30T08:59:59.995", 115, true},
                {"2002-05-30T08:59:59.995", 135, true},
                {"2002-05-30T08:59:59.995", 136, false},
                {"2002-05-30T08:59:59.996", 124, false},
            };
            expectedValidator = new IncludesValidator(5L, 20L, 10L, 30L);
            assertExpression(env, seedTime, 100, "a.includes(b, 5 milliseconds, 20 milliseconds, 10 milliseconds, 30 milliseconds)", expected, expectedValidator);
        }
    }

    private static class ExprDTIntervalMeetsWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new MeetsValidator();
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T08:59:59.000", 1000, true},
                {"2002-05-30T08:59:59.000", 1001, false},
                {"2002-05-30T08:59:59.998", 1, false},
                {"2002-05-30T08:59:59.999", 1, true},
                {"2002-05-30T09:00:00.000", 0, true},
                {"2002-05-30T09:00:00.000", 1, false},
                {"2002-05-30T09:00:00.001", 0, false},
            };
            assertExpression(env, seedTime, 0, "a.meets(b)", expected, expectedValidator);

            // test 1-parameter form
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 0, false},
                {"2002-05-30T08:59:59.000", 994, false},
                {"2002-05-30T08:59:59.000", 995, true},
                {"2002-05-30T08:59:59.000", 1005, true},
                {"2002-05-30T08:59:59.000", 1006, false},
                {"2002-05-30T08:59:59.994", 0, false},
                {"2002-05-30T08:59:59.994", 1, true},
                {"2002-05-30T08:59:59.995", 0, true},
                {"2002-05-30T08:59:59.999", 0, true},
                {"2002-05-30T08:59:59.999", 1, true},
                {"2002-05-30T08:59:59.999", 6, true},
                {"2002-05-30T08:59:59.999", 7, false},
                {"2002-05-30T09:00:00.000", 0, true},
                {"2002-05-30T09:00:00.000", 1, true},
                {"2002-05-30T09:00:00.000", 5, true},
                {"2002-05-30T09:00:00.005", 0, true},
                {"2002-05-30T09:00:00.005", 1, false},
            };
            expectedValidator = new MeetsValidator(5L);
            assertExpression(env, seedTime, 0, "a.meets(b, 5 milliseconds)", expected, expectedValidator);
        }
    }

    private static class ExprDTIntervalMetByWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new MetByValidator();
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T09:00:00.990", 0, false},
                {"2002-05-30T09:00:00.100", 0, true},
                {"2002-05-30T09:00:00.100", 500, true},
                {"2002-05-30T09:00:00.101", 0, false},
            };
            assertExpression(env, seedTime, 100, "a.metBy(b)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T08:59:59.999", 1, false},
                {"2002-05-30T09:00:00.000", 0, true},
                {"2002-05-30T09:00:00.000", 1, true},
            };
            assertExpression(env, seedTime, 0, "a.metBy(b)", expected, expectedValidator);

            // test 1-parameter form
            expected = new Object[][]{
                {"2002-05-30T08:59:59.994", 0, false},
                {"2002-05-30T08:59:59.994", 5, false},
                {"2002-05-30T08:59:59.995", 0, true},
                {"2002-05-30T09:00:00.000", 0, true},
                {"2002-05-30T09:00:00.000", 20, true},
                {"2002-05-30T09:00:00.005", 0, true},
                {"2002-05-30T09:00:00.005", 1000, true},
                {"2002-05-30T09:00:00.006", 0, false},
            };
            expectedValidator = new MetByValidator(5L);
            assertExpression(env, seedTime, 0, "a.metBy(b, 5 milliseconds)", expected, expectedValidator);

            expected = new Object[][]{
                {"2002-05-30T08:59:59.994", 0, false},
                {"2002-05-30T08:59:59.994", 5, false},
                {"2002-05-30T08:59:59.995", 0, false},
                {"2002-05-30T09:00:00.094", 0, false},
                {"2002-05-30T09:00:00.095", 0, true},
                {"2002-05-30T09:00:00.105", 0, true},
                {"2002-05-30T09:00:00.105", 5000, true},
                {"2002-05-30T09:00:00.106", 0, false},
            };
            expectedValidator = new MetByValidator(5L);
            assertExpression(env, seedTime, 100, "a.metBy(b, 5 milliseconds)", expected, expectedValidator);
        }
    }

    private static class ExprDTIntervalOverlapsWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new OverlapsValidator();
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T08:59:59.000", 1000, false},
                {"2002-05-30T08:59:59.000", 1001, true},
                {"2002-05-30T08:59:59.000", 1050, true},
                {"2002-05-30T08:59:59.000", 1099, true},
                {"2002-05-30T08:59:59.000", 1100, false},
                {"2002-05-30T08:59:59.999", 1, false},
                {"2002-05-30T08:59:59.999", 2, true},
                {"2002-05-30T08:59:59.999", 100, true},
                {"2002-05-30T08:59:59.999", 101, false},
                {"2002-05-30T09:00:00.000", 0, false},
            };
            assertExpression(env, seedTime, 100, "a.overlaps(b)", expected, expectedValidator);

            // test 1-parameter form (overlap by not more then X msec)
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 1000, false},
                {"2002-05-30T08:59:59.000", 1001, true},
                {"2002-05-30T08:59:59.000", 1005, true},
                {"2002-05-30T08:59:59.000", 1006, false},
                {"2002-05-30T08:59:59.000", 1100, false},
                {"2002-05-30T08:59:59.999", 1, false},
                {"2002-05-30T08:59:59.999", 2, true},
                {"2002-05-30T08:59:59.999", 6, true},
                {"2002-05-30T08:59:59.999", 7, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 5, false},
            };
            expectedValidator = new OverlapsValidator(5L);
            assertExpression(env, seedTime, 100, "a.overlaps(b, 5 milliseconds)", expected, expectedValidator);

            // test 2-parameter form (overlap by min X and not more then Y msec)
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 1004, false},
                {"2002-05-30T08:59:59.000", 1005, true},
                {"2002-05-30T08:59:59.000", 1010, true},
                {"2002-05-30T08:59:59.000", 1011, false},
                {"2002-05-30T08:59:59.999", 5, false},
                {"2002-05-30T08:59:59.999", 6, true},
                {"2002-05-30T08:59:59.999", 11, true},
                {"2002-05-30T08:59:59.999", 12, false},
                {"2002-05-30T08:59:59.999", 12, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 5, false},
            };
            expectedValidator = new OverlapsValidator(5L, 10L);
            assertExpression(env, seedTime, 100, "a.overlaps(b, 5 milliseconds, 10 milliseconds)", expected, expectedValidator);
        }
    }

    private static class ExprDTIntervalOverlappedByWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new OverlappedByValidator();
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T08:59:59.000", 1000, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 1, false},
                {"2002-05-30T09:00:00.001", 99, false},
                {"2002-05-30T09:00:00.001", 100, true},
                {"2002-05-30T09:00:00.099", 1, false},
                {"2002-05-30T09:00:00.099", 2, true},
                {"2002-05-30T09:00:00.100", 0, false},
                {"2002-05-30T09:00:00.100", 1, false},
            };
            assertExpression(env, seedTime, 100, "a.overlappedBy(b)", expected, expectedValidator);

            // test 1-parameter form (overlap by not more then X msec)
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 1000, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 1, false},
                {"2002-05-30T09:00:00.001", 99, false},
                {"2002-05-30T09:00:00.094", 7, false},
                {"2002-05-30T09:00:00.094", 100, false},
                {"2002-05-30T09:00:00.095", 5, false},
                {"2002-05-30T09:00:00.095", 6, true},
                {"2002-05-30T09:00:00.095", 100, true},
                {"2002-05-30T09:00:00.099", 1, false},
                {"2002-05-30T09:00:00.099", 2, true},
                {"2002-05-30T09:00:00.099", 100, true},
                {"2002-05-30T09:00:00.100", 100, false},
            };
            expectedValidator = new OverlappedByValidator(5L);
            assertExpression(env, seedTime, 100, "a.overlappedBy(b, 5 milliseconds)", expected, expectedValidator);

            // test 2-parameter form (overlap by min X and not more then Y msec)
            expected = new Object[][]{
                {"2002-05-30T08:59:59.000", 1000, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 1, false},
                {"2002-05-30T09:00:00.001", 99, false},
                {"2002-05-30T09:00:00.089", 14, false},
                {"2002-05-30T09:00:00.090", 10, false},
                {"2002-05-30T09:00:00.090", 11, true},
                {"2002-05-30T09:00:00.090", 1000, true},
                {"2002-05-30T09:00:00.095", 5, false},
                {"2002-05-30T09:00:00.095", 6, true},
                {"2002-05-30T09:00:00.096", 5, false},
                {"2002-05-30T09:00:00.096", 100, false},
                {"2002-05-30T09:00:00.100", 100, false},
            };
            expectedValidator = new OverlappedByValidator(5L, 10L);
            assertExpression(env, seedTime, 100, "a.overlappedBy(b, 5 milliseconds, 10 milliseconds)", expected, expectedValidator);
        }
    }

    private static class ExprDTIntervalStartsWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new StartsValidator();
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T08:59:59.999", 100, false},
                {"2002-05-30T09:00:00.000", 0, true},
                {"2002-05-30T09:00:00.000", 1, true},
                {"2002-05-30T09:00:00.000", 99, true},
                {"2002-05-30T09:00:00.000", 100, false},
                {"2002-05-30T09:00:00.001", 0, false},
            };
            assertExpression(env, seedTime, 100, "a.starts(b)", expected, expectedValidator);

            // test 1-parameter form (max distance between start times)
            expected = new Object[][]{
                {"2002-05-30T08:59:59.994", 6, false},
                {"2002-05-30T08:59:59.995", 0, true},
                {"2002-05-30T08:59:59.995", 104, true},
                {"2002-05-30T08:59:59.995", 105, false},
                {"2002-05-30T09:00:00.000", 0, true},
                {"2002-05-30T09:00:00.000", 1, true},
                {"2002-05-30T09:00:00.000", 99, true},
                {"2002-05-30T09:00:00.000", 100, false},
                {"2002-05-30T09:00:00.001", 0, true},
                {"2002-05-30T09:00:00.005", 94, true},
                {"2002-05-30T09:00:00.005", 95, false},
                {"2002-05-30T09:00:00.005", 100, false},
            };
            expectedValidator = new StartsValidator(5L);
            assertExpression(env, seedTime, 100, "a.starts(b, 5 milliseconds)", expected, expectedValidator);
        }
    }

    private static class ExprDTIntervalStartedByWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Validator expectedValidator = new StartedByValidator();
            String seedTime = "2002-05-30T09:00:00.000";
            Object[][] expected = {
                {"2002-05-30T08:59:59.999", 100, false},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 100, false},
                {"2002-05-30T09:00:00.000", 101, true},
                {"2002-05-30T09:00:00.001", 0, false},
                {"2002-05-30T09:00:00.001", 101, false},
            };
            assertExpression(env, seedTime, 100, "a.startedBy(b)", expected, expectedValidator);

            // test 1-parameter form (max distance between start times)
            expected = new Object[][]{
                {"2002-05-30T08:59:59.994", 6, false},
                {"2002-05-30T08:59:59.995", 0, false},
                {"2002-05-30T08:59:59.995", 105, false},
                {"2002-05-30T08:59:59.995", 106, true},
                {"2002-05-30T09:00:00.000", 0, false},
                {"2002-05-30T09:00:00.000", 100, false},
                {"2002-05-30T09:00:00.000", 101, true},
                {"2002-05-30T09:00:00.001", 99, false},
                {"2002-05-30T09:00:00.001", 100, true},
                {"2002-05-30T09:00:00.005", 94, false},
                {"2002-05-30T09:00:00.005", 95, false},
                {"2002-05-30T09:00:00.005", 96, true},
            };
            expectedValidator = new StartedByValidator(5L);
            assertExpression(env, seedTime, 100, "a.startedBy(b, 5 milliseconds)", expected, expectedValidator);
        }
    }

    private static void setVStartEndVariables(RegressionEnvironment env, long vstart, long vend) {
        env.runtime().getVariableService().setVariableValue(null, "V_START", vstart);
        env.runtime().getVariableService().setVariableValue(null, "V_END", vend);
    }

    private static void assertExpression(RegressionEnvironment env, String seedTime, long seedDuration, String whereClause, Object[][] timestampsAndResult, Validator validator) {
        for (SupportDateTimeFieldType fieldType : SupportDateTimeFieldType.values()) {
            assertExpressionForType(env, seedTime, seedDuration, whereClause, timestampsAndResult, validator, fieldType);
        }
    }

    private static void assertExpressionForType(RegressionEnvironment env, String seedTime, long seedDuration, String whereClause, Object[][] timestampsAndResult, Validator validator, SupportDateTimeFieldType fieldType) {

        String epl = "@name('s0') select * from A_" + fieldType.name() + "#lastevent as a, B_" + fieldType.name() + "#lastevent as b " + "where " + whereClause;
        env.compileDeploy(epl).addListener("s0");

        env.sendEventObjectArray(new Object[]{fieldType.makeStart(seedTime), fieldType.makeEnd(seedTime, seedDuration)}, "B_" + fieldType.name());

        for (Object[] test : timestampsAndResult) {
            String testtime = (String) test[0];
            Long testduration = ((Number) test[1]).longValue();
            boolean expected = (Boolean) test[2];

            long rightStart = DateTime.parseDefaultMSec(seedTime);
            long rightEnd = rightStart + seedDuration;
            long leftStart = DateTime.parseDefaultMSec(testtime);
            long leftEnd = leftStart + testduration;
            String message = "time " + testtime + " duration " + testduration + " for '" + whereClause + "'";

            if (validator != null) {
                assertEquals("Validation of expected result failed for " + message, expected, validator.validate(leftStart, leftEnd, rightStart, rightEnd));
            }

            env.sendEventObjectArray(new Object[]{fieldType.makeStart(testtime), fieldType.makeEnd(testtime, testduration)}, "A_" + fieldType.name());

            if (!env.listener("s0").isInvoked() && expected) {
                fail("Expected but not received for " + message);
            }
            if (env.listener("s0").isInvoked() && !expected) {
                fail("Not expected but received for " + message);
            }
            env.listener("s0").reset();
        }

        env.undeployAll();
    }

    private static long getMillisecForDays(int days) {
        return days * 24 * 60 * 60 * 1000L;
    }

    private static void assertExpressionBean(RegressionEnvironment env, String seedTime, long seedDuration, String whereClause, Object[][] timestampsAndResult, Validator validator) {

        String epl = "@name('s0') select * from SupportTimeStartEndA#lastevent as a, SupportTimeStartEndB#lastevent as b where " + whereClause;
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(SupportTimeStartEndB.make("B", seedTime, seedDuration));

        for (Object[] test : timestampsAndResult) {
            String testtime = (String) test[0];
            Long testduration = ((Number) test[1]).longValue();
            boolean expected = (Boolean) test[2];

            long rightStart = DateTime.parseDefaultMSec(seedTime);
            long rightEnd = rightStart + seedDuration;
            long leftStart = DateTime.parseDefaultMSec(testtime);
            long leftEnd = leftStart + testduration;
            String message = "time " + testtime + " duration " + testduration + " for '" + whereClause + "'";

            if (validator != null) {
                assertEquals("Validation of expected result failed for " + message, expected, validator.validate(leftStart, leftEnd, rightStart, rightEnd));
            }

            env.sendEventBean(SupportTimeStartEndA.make("A", testtime, testduration));

            if (!env.listener("s0").isInvoked() && expected) {
                fail("Expected but not received for " + message);
            }
            if (env.listener("s0").isInvoked() && !expected) {
                fail("Not expected but received for " + message);
            }
            env.listener("s0").reset();
        }

        env.undeployAll();
    }

    private interface Validator {
        boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd);
    }

    private static class BeforeValidator implements Validator {
        private Long start;
        private Long end;

        private BeforeValidator(Long start, Long end) {
            this.start = start;
            this.end = end;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {
            long delta = rightStart - leftEnd;
            return start <= delta && delta <= end;
        }
    }

    private static class AfterValidator implements Validator {
        private Long start;
        private Long end;

        private AfterValidator(Long start, Long end) {
            this.start = start;
            this.end = end;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {
            long delta = leftStart - rightEnd;
            return start <= delta && delta <= end;
        }
    }

    private static class CoincidesValidator implements Validator {
        private final Long startThreshold;
        private final Long endThreshold;

        private CoincidesValidator() {
            startThreshold = 0L;
            endThreshold = 0L;
        }

        private CoincidesValidator(Long startThreshold) {
            this.startThreshold = startThreshold;
            this.endThreshold = startThreshold;
        }

        private CoincidesValidator(Long startThreshold, Long endThreshold) {
            this.startThreshold = startThreshold;
            this.endThreshold = endThreshold;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {
            long startDelta = Math.abs(leftStart - rightStart);
            long endDelta = Math.abs(leftEnd - rightEnd);
            return startDelta <= startThreshold && endDelta <= endThreshold;
        }
    }

    private static class DuringValidator implements Validator {

        private int form;
        private Long threshold;
        private Long minThreshold;
        private Long maxThreshold;
        private Long minStartThreshold;
        private Long maxStartThreshold;
        private Long minEndThreshold;
        private Long maxEndThreshold;

        private DuringValidator() {
            form = 1;
        }

        private DuringValidator(Long threshold) {
            form = 2;
            this.threshold = threshold;
        }

        private DuringValidator(Long minThreshold, Long maxThreshold) {
            form = 3;
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
        }

        private DuringValidator(Long minStartThreshold, Long maxStartThreshold, Long minEndThreshold, Long maxEndThreshold) {
            form = 4;
            this.minStartThreshold = minStartThreshold;
            this.maxStartThreshold = maxStartThreshold;
            this.minEndThreshold = minEndThreshold;
            this.maxEndThreshold = maxEndThreshold;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {
            if (form == 1) {
                return rightStart < leftStart &&
                    leftEnd < rightEnd;
            } else if (form == 2) {
                long distanceStart = leftStart - rightStart;
                if (distanceStart <= 0 || distanceStart > threshold) {
                    return false;
                }
                long distanceEnd = rightEnd - leftEnd;
                return !(distanceEnd <= 0 || distanceEnd > threshold);
            } else if (form == 3) {
                long distanceStart = leftStart - rightStart;
                if (distanceStart < minThreshold || distanceStart > maxThreshold) {
                    return false;
                }
                long distanceEnd = rightEnd - leftEnd;
                return !(distanceEnd < minThreshold || distanceEnd > maxThreshold);
            } else if (form == 4) {
                long distanceStart = leftStart - rightStart;
                if (distanceStart < minStartThreshold || distanceStart > maxStartThreshold) {
                    return false;
                }
                long distanceEnd = rightEnd - leftEnd;
                return !(distanceEnd < minEndThreshold || distanceEnd > maxEndThreshold);
            }
            throw new IllegalStateException("Invalid form: " + form);
        }
    }

    private static class FinishesValidator implements Validator {
        private Long threshold;

        private FinishesValidator() {
        }

        private FinishesValidator(Long threshold) {
            this.threshold = threshold;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {
            if (threshold == null) {
                return rightStart < leftStart && leftEnd == rightEnd;
            } else {
                if (rightStart >= leftStart) {
                    return false;
                }
                long delta = Math.abs(leftEnd - rightEnd);
                return delta <= threshold;
            }
        }
    }

    private static class FinishedByValidator implements Validator {
        private Long threshold;

        private FinishedByValidator() {
        }

        private FinishedByValidator(Long threshold) {
            this.threshold = threshold;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {

            if (threshold == null) {
                return leftStart < rightStart && leftEnd == rightEnd;
            } else {
                if (leftStart >= rightStart) {
                    return false;
                }
                long delta = Math.abs(leftEnd - rightEnd);
                return delta <= threshold;
            }
        }
    }

    private static class IncludesValidator implements Validator {

        private int form;
        private Long threshold;
        private Long minThreshold;
        private Long maxThreshold;
        private Long minStartThreshold;
        private Long maxStartThreshold;
        private Long minEndThreshold;
        private Long maxEndThreshold;

        private IncludesValidator() {
            form = 1;
        }

        private IncludesValidator(Long threshold) {
            form = 2;
            this.threshold = threshold;
        }

        private IncludesValidator(Long minThreshold, Long maxThreshold) {
            form = 3;
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
        }

        private IncludesValidator(Long minStartThreshold, Long maxStartThreshold, Long minEndThreshold, Long maxEndThreshold) {
            form = 4;
            this.minStartThreshold = minStartThreshold;
            this.maxStartThreshold = maxStartThreshold;
            this.minEndThreshold = minEndThreshold;
            this.maxEndThreshold = maxEndThreshold;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {

            if (form == 1) {
                return leftStart < rightStart &&
                    rightEnd < leftEnd;
            } else if (form == 2) {
                long distanceStart = rightStart - leftStart;
                if (distanceStart <= 0 || distanceStart > threshold) {
                    return false;
                }
                long distanceEnd = leftEnd - rightEnd;
                return !(distanceEnd <= 0 || distanceEnd > threshold);
            } else if (form == 3) {
                long distanceStart = rightStart - leftStart;
                if (distanceStart < minThreshold || distanceStart > maxThreshold) {
                    return false;
                }
                long distanceEnd = leftEnd - rightEnd;
                return !(distanceEnd < minThreshold || distanceEnd > maxThreshold);
            } else if (form == 4) {
                long distanceStart = rightStart - leftStart;
                if (distanceStart < minStartThreshold || distanceStart > maxStartThreshold) {
                    return false;
                }
                long distanceEnd = leftEnd - rightEnd;
                return !(distanceEnd < minEndThreshold || distanceEnd > maxEndThreshold);
            }
            throw new IllegalStateException("Invalid form: " + form);
        }
    }

    private static class MeetsValidator implements Validator {
        private Long threshold;

        private MeetsValidator() {
        }

        private MeetsValidator(Long threshold) {
            this.threshold = threshold;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {

            if (threshold == null) {
                return rightStart == leftEnd;
            } else {
                long delta = Math.abs(rightStart - leftEnd);
                return delta <= threshold;
            }
        }
    }

    private static class MetByValidator implements Validator {
        private Long threshold;

        private MetByValidator() {
        }

        private MetByValidator(Long threshold) {
            this.threshold = threshold;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {

            if (threshold == null) {
                return leftStart == rightEnd;
            } else {
                long delta = Math.abs(leftStart - rightEnd);
                return delta <= threshold;
            }
        }
    }

    private static class OverlapsValidator implements Validator {
        private int form;
        private Long threshold;
        private Long minThreshold;
        private Long maxThreshold;

        private OverlapsValidator() {
            form = 1;
        }

        private OverlapsValidator(Long threshold) {
            form = 2;
            this.threshold = threshold;
        }

        private OverlapsValidator(Long minThreshold, Long maxThreshold) {
            form = 3;
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {

            boolean match = (leftStart < rightStart) &&
                (rightStart < leftEnd) &&
                (leftEnd < rightEnd);

            if (form == 1) {
                return match;
            } else if (form == 2) {
                if (!match) {
                    return false;
                }
                long delta = leftEnd - rightStart;
                return 0 <= delta && delta <= threshold;
            } else if (form == 3) {
                if (!match) {
                    return false;
                }
                long delta = leftEnd - rightStart;
                return minThreshold <= delta && delta <= maxThreshold;
            }
            throw new IllegalArgumentException("Invalid form " + form);
        }
    }

    private static class OverlappedByValidator implements Validator {
        private int form;
        private Long threshold;
        private Long minThreshold;
        private Long maxThreshold;

        private OverlappedByValidator() {
            form = 1;
        }

        private OverlappedByValidator(Long threshold) {
            form = 2;
            this.threshold = threshold;
        }

        private OverlappedByValidator(Long minThreshold, Long maxThreshold) {
            form = 3;
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {

            boolean match = (rightStart < leftStart) &&
                (leftStart < rightEnd) &&
                (rightEnd < leftEnd);

            if (form == 1) {
                return match;
            } else if (form == 2) {
                if (!match) {
                    return false;
                }
                long delta = rightEnd - leftStart;
                return 0 <= delta && delta <= threshold;
            } else if (form == 3) {
                if (!match) {
                    return false;
                }
                long delta = rightEnd - leftStart;
                return minThreshold <= delta && delta <= maxThreshold;
            }
            throw new IllegalArgumentException("Invalid form " + form);
        }
    }

    private static class StartsValidator implements Validator {
        private Long threshold;

        private StartsValidator() {
        }

        private StartsValidator(Long threshold) {
            this.threshold = threshold;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {
            if (threshold == null) {
                return (leftStart == rightStart) && (leftEnd < rightEnd);
            } else {
                long delta = Math.abs(leftStart - rightStart);
                return (delta <= threshold) && (leftEnd < rightEnd);
            }
        }
    }

    private static class StartedByValidator implements Validator {
        private Long threshold;

        private StartedByValidator() {
        }

        private StartedByValidator(Long threshold) {
            this.threshold = threshold;
        }

        public boolean validate(long leftStart, long leftEnd, long rightStart, long rightEnd) {
            if (threshold == null) {
                return (leftStart == rightStart) && (leftEnd > rightEnd);
            } else {
                long delta = Math.abs(leftStart - rightStart);
                return (delta <= threshold) && (leftEnd > rightEnd);
            }
        }
    }
}
