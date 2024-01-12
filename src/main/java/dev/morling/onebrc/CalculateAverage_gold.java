/*
 *  Copyright 2023 The original authors
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
package dev.morling.onebrc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import dev.morling.onebrc.data.MeasurementAggregation;

public class CalculateAverage_gold {

    private static final String FILE = "./measurements.txt";

  private static record Measurement(String station, double value) {
    private Measurement(String[] parts) {
      this(parts[0], Double.parseDouble(parts[1]));
    }
  }

    public static void main(String[] args) throws IOException {
        // use a Map since insertion/contains is going to happen K times. Laster we need ordering which is NlgN
        // TODO: See what happens if we just get a ConcurrentTreeMap instead (compare the runtimes)
        final Map<String, MeasurementAggregation> aggregates = new ConcurrentHashMap<>();

        try (final Stream<String> lines = Files.lines(Paths.get(FILE))) {
            lines.parallel().forEach(l -> {
                final Measurement measure = new Measurement(l.split(";"));

                if (aggregates.containsKey(measure.station)) {
                    MeasurementAggregation aggregation = aggregates.get(measure.station);
                    aggregation.appendValue(measure.value);
                    // don't need to insert since we are using the same object reference
                    return;
                }
                MeasurementAggregation aggregation = new MeasurementAggregation(measure.value);
                aggregates.put(measure.station, aggregation);
            });
        }

        // TODO: would it be more effiencent to use a map and collection pattern instead of the forEach?

        // now use a TreeMap to build the ordering of the hashMap in NlgN time
        final Map<String, MeasurementAggregation> aggregatesSorted = new TreeMap<>(aggregates);
        System.out.println(aggregatesSorted);
    }
}

// Attempt #1: Just parrallelize with HashMap and TreeMap
// Attempt #2: parrallel with TreeMap only.