/**
 *  Copyright 2020 The JfrUnit authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package dev.morling.jfrunit;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class JfrUnitQuarkusLifecycleCallback implements QuarkusTestBeforeEachCallback, QuarkusTestAfterEachCallback {

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        Object instance = context.getTestInstance();
        List<String> enabledEvents = getEnabledEvents(context.getTestMethod());

        List<JfrEvents> allJfrEvents = getJfrEvents(instance);
        for (JfrEvents jfrEvents : allJfrEvents) {
            jfrEvents.startRecordingEvents(enabledEvents, context.getTestMethod());
        }
    }

    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        Object instance = context.getTestInstance();

        List<JfrEvents> allJfrEvents = getJfrEvents(instance);
        for (JfrEvents jfrEvents : allJfrEvents) {
            jfrEvents.stopRecordingEvents();
        }
    }

    private List<String> getEnabledEvents(Method testMethod) {
        return Arrays.stream(testMethod.getAnnotationsByType(EnableEvent.class))
            .map(EnableEvent::value)
            .collect(Collectors.toList());
    }

    private List<JfrEvents> getJfrEvents(Object instance) {
        return Arrays.stream(instance.getClass().getDeclaredFields())
            .filter(f -> f.getType() == JfrEvents.class)
            .map(f -> {
                try {
                    return (JfrEvents) f.get(instance);
                }
                catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());
    }
}
