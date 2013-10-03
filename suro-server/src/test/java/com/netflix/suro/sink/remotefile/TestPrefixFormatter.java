/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.suro.sink.remotefile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.suro.jackson.DefaultObjectMapper;
import com.netflix.suro.sink.RemotePrefixFormatter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestPrefixFormatter {
    private static Injector injector = Guice.createInjector(
            new RemoteFileSuroPlugin(),
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(ObjectMapper.class).to(DefaultObjectMapper.class);
                }
            }
        );

    @Test
    public void testStatic() throws IOException {
        String spec = "{\n" +
                "    \"type\": \"static\",\n" +
                "    \"prefix\": \"prefix\"\n" +
                "}";

        ObjectMapper mapper = injector.getInstance(ObjectMapper.class);
        RemotePrefixFormatter formatter = mapper.readValue(spec, new TypeReference<RemotePrefixFormatter>(){});
        assertEquals(formatter.get(), "prefix");
    }

    @Test
    public void testDateRegionStack() throws IOException {
        String spec = "{\n" +
                "    \"type\": \"DateRegionStack\",\n" +
                "    \"date\": \"YYYYMMDD\",\n" +
                "    \"region\": \"us-east-1\",\n" +
                "    \"stack\": \"normal\"\n" +
                "}";

        ObjectMapper mapper = injector.getInstance(ObjectMapper.class);
        mapper.setInjectableValues(new InjectableValues() {
            @Override
            public Object findInjectableValue(Object valueId, DeserializationContext ctxt, BeanProperty forProperty, Object beanInstance) {
                return null;
            }
        });
        RemotePrefixFormatter formatter = mapper.readValue(spec, new TypeReference<RemotePrefixFormatter>() {});
        DateTimeFormatter format = DateTimeFormat.forPattern("YYYYMMDD");
        String answer = String.format("%s/us-east-1/normal/", format.print(new DateTime()));
        assertEquals(formatter.get(), answer);
    }

    @Test
    public void testInjectedDateRegionStack() throws IOException {
        String spec = "{\n" +
                "    \"type\": \"DateRegionStack\",\n" +
                "    \"date\": \"YYYYMMDD\"\n" +
                "}";
        final Map<String, Object> injectables = Maps.newHashMap();
        injectables.put("region", "eu-west-1");
        injectables.put("stack", "gps");

        ObjectMapper mapper = injector.getInstance(ObjectMapper.class);
        mapper.setInjectableValues(new InjectableValues() {
            @Override
            public Object findInjectableValue(Object valueId, DeserializationContext ctxt, BeanProperty forProperty, Object beanInstance) {
                return injectables.get(valueId);
            }
        });
        RemotePrefixFormatter formatter = mapper.readValue(spec, new TypeReference<RemotePrefixFormatter>() {});
        DateTimeFormatter format = DateTimeFormat.forPattern("YYYYMMDD");
        String answer = String.format("%s/eu-west-1/gps/", format.print(new DateTime()));
        assertEquals(formatter.get(), answer);
    }
}
