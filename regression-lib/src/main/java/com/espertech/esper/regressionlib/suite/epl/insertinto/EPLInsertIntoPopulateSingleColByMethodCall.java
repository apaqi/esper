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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.core.AvroGenericDataBackedEventBean;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.core.MappedEventBean;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;
import com.espertech.esper.common.internal.event.core.WrapperEventType;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionlib.support.events.SupportEventInfra;
import org.apache.avro.generic.GenericData;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.regressionlib.support.events.SupportEventInfra.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EPLInsertIntoPopulateSingleColByMethodCall implements RegressionExecution {
    public void run(RegressionEnvironment env) {

        // Bean
        runAssertionConversionImplicitType(env, "Bean", "SupportBean", "convertEvent", BeanEventType.class, SupportBean.class,
            "SupportMarketDataBean", new SupportMarketDataBean("ACME", 0, 0L, null), FBEANWTYPE, "theString".split(","), new Object[]{"ACME"});

        // Map
        Map<String, Object> mapEventOne = new HashMap<>();
        mapEventOne.put("one", "1");
        mapEventOne.put("two", "2");
        runAssertionConversionImplicitType(env, "Map", "MapOne", "convertEventMap", WrapperEventType.class, Map.class,
            "MapTwo", mapEventOne, FMAPWTYPE, "one,two".split(","), new Object[]{"1", "|2|"});

        Map<String, Object> mapEventTwo = new HashMap<>();
        mapEventTwo.put("one", "3");
        mapEventTwo.put("two", "4");
        runAssertionConversionConfiguredType(env, "MapOne", "convertEventMap", "MapTwo", MappedEventBean.class, HashMap.class, mapEventTwo, FMAPWTYPE, "one,two".split(","), new Object[]{"3", "|4|"});

        // Object-Array
        runAssertionConversionImplicitType(env, "OA", "OAOne", "convertEventObjectArray", WrapperEventType.class, Object[].class,
            "OATwo", new Object[]{"1", "2"}, FOAWTYPE, "one,two".split(","), new Object[]{"1", "|2|"});
        runAssertionConversionConfiguredType(env, "OAOne", "convertEventObjectArray", "OATwo", ObjectArrayBackedEventBean.class, Object[].class, new Object[]{"3", "4"}, FOAWTYPE, "one,two".split(","), new Object[]{"3", "|4|"});

        // Avro
        GenericData.Record rowOne = new GenericData.Record(AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("AvroOne")));
        rowOne.put("one", "1");
        rowOne.put("two", "2");
        runAssertionConversionImplicitType(env, "Avro", "AvroOne", "convertEventAvro", WrapperEventType.class, GenericData.Record.class,
            "AvroTwo", rowOne, FAVROWTYPE, "one,two".split(","), new Object[]{"1", "|2|"});

        GenericData.Record rowTwo = new GenericData.Record(AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("AvroTwo")));
        rowTwo.put("one", "3");
        rowTwo.put("two", "4");
        runAssertionConversionConfiguredType(env, "AvroOne", "convertEventAvro", "AvroTwo", AvroGenericDataBackedEventBean.class, GenericData.Record.class, rowTwo, FAVROWTYPE, "one,two".split(","), new Object[]{"3", "|4|"});
    }

    private static void runAssertionConversionImplicitType(RegressionEnvironment env, String prefix,
                                                           String typeNameOrigin,
                                                           String functionName,
                                                           Class eventTypeType,
                                                           Class underlyingType,
                                                           String typeNameEvent,
                                                           Object event,
                                                           SupportEventInfra.FunctionSendEventWType sendEvent,
                                                           String[] propertyName,
                                                           Object[] propertyValues) {
        String streamName = prefix + "_Stream";
        String textOne = "@name('s1') insert into " + streamName + " select * from " + typeNameOrigin;
        String textTwo = "@name('s2') insert into " + streamName + " select " + SupportStaticMethodLib.class.getName() + "." + functionName + "(s0) from " + typeNameEvent + " as s0";

        RegressionPath path = new RegressionPath();
        env.compileDeploy(textOne, path).addListener("s1");
        EventType type = env.statement("s1").getEventType();
        assertEquals(underlyingType, type.getUnderlyingType());

        env.compileDeploy(textTwo, path).addListener("s2");
        type = env.statement("s2").getEventType();
        assertEquals(underlyingType, type.getUnderlyingType());

        sendEvent.apply(env, event, typeNameEvent);

        EventBean theEvent = env.listener("s2").assertOneGetNewAndReset();
        assertTrue(JavaClassHelper.isSubclassOrImplementsInterface(theEvent.getEventType().getClass(), eventTypeType));
        assertTrue(JavaClassHelper.isSubclassOrImplementsInterface(theEvent.getUnderlying().getClass(), underlyingType));
        EPAssertionUtil.assertProps(theEvent, propertyName, propertyValues);

        env.undeployAll();
    }

    private static void runAssertionConversionConfiguredType(RegressionEnvironment env, String typeNameTarget,
                                                             String functionName,
                                                             String typeNameOrigin,
                                                             Class eventBeanType,
                                                             Class underlyingType,
                                                             Object event,
                                                             FunctionSendEventWType sendEvent,
                                                             String[] propertyName,
                                                             Object[] propertyValues) {

        // test native
        env.compileDeploy("insert into " + typeNameTarget + " select " + SupportStaticMethodLib.class.getName() + "." + functionName + "(s0) from " + typeNameOrigin + " as s0");
        env.compileDeploy("@name('s0') select * from " + typeNameTarget).addListener("s0");

        sendEvent.apply(env, event, typeNameOrigin);

        EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
        assertTrue(JavaClassHelper.isSubclassOrImplementsInterface(eventBean.getUnderlying().getClass(), underlyingType));
        assertTrue(JavaClassHelper.isSubclassOrImplementsInterface(eventBean.getClass(), eventBeanType));
        EPAssertionUtil.assertProps(eventBean, propertyName, propertyValues);

        env.undeployAll();
    }
}