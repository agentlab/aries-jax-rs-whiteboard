/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.jax.rs.whiteboard.internal;

import org.apache.aries.component.dsl.OSGiResult;
import org.apache.aries.component.dsl.Publisher;
import org.osgi.framework.Filter;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FilteredPublisher<T> implements AutoCloseable {

    public FilteredPublisher(Publisher<? super T> publisher, Filter filter) {
        _publisher = publisher;
        _filter = filter;
    }

    public void close() {
        if (_closed.compareAndSet(false, true)) {
            _results.forEach((__, result) -> result.close());

            _results.clear();
        }
    }

    public void publishIfMatched(T t, Map<String, ?> properties) {
        if (_closed.get()) {
            return;
        }

        if (_filter.matches(properties)) {
            OSGiResult result = _publisher.publish(t);

            OSGiResult old = _results.put(t, result);

            if (old != null) {
                old.close();
            }

            if (_closed.get()) {
                result.close();
            }
        }
    }

    public void retract(T t) {
        if (_closed.get()) {
            return;
        }

        OSGiResult result = _results.remove(t);

        if (result != null) {
            result.close();
        }
    }

    private Publisher<? super T> _publisher;
    private Filter _filter;
    private AtomicBoolean _closed = new AtomicBoolean(false);
    private IdentityHashMap<T, OSGiResult> _results = new IdentityHashMap<>();

}
