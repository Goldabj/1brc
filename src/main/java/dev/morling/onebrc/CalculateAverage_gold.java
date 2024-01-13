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
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import dev.morling.onebrc.data.MeasurementAggregation;
import dev.morling.onebrc.files.FastFiles;

public class CalculateAverage_gold {

    private static final String FILE = "./measurements.txt";

  private static record Measurement(String station, double value) {
    private Measurement(String[] parts) {
      this(parts[0], Double.parseDouble(parts[1]));
    }
  }

    public static void main(String[] args) throws IOException {
        // Page size is 16KB. MMap will allocate a page of mem on each reach. Therefore this is the max parallel throughput we get
        // without duplicating mmap calls.
        final Long macPageSize = 16384L;

        // use a Map since insertion/contains is going to happen K times. Laster we need ordering which is NlgN
        final Map<String, MeasurementAggregation> aggregates = new ConcurrentHashMap<>();

        final Stream<ByteBuffer> fileChunks = FastFiles.readMMapChunks(Paths.get(FILE), macPageSize);
        fileChunks.forEach(chunk -> {

            final Stream<String> lines = FastFiles.lines(chunk);
            lines.forEach(l -> {
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
        });

        final Map<String, MeasurementAggregation> aggregationsSorted = new TreeMap<>(aggregates);
        System.out.println(aggregationsSorted);
    }
}

// Attempt #1: Just parrallelize with HashMap and TreeMap
// Attempt #2: parrallel with TreeMap only.
// Attempt #3: Using Java NIO to create a memory mapped file when reading the file by mapping 64mB chunks.

// TODO: Attemp #4: Make smaller file chunks that align with my page size
// TODO: Attempt #5: Create chunks based on the number of available processors
// TODO: Attemp #6: instead of parralle() try virtual threads