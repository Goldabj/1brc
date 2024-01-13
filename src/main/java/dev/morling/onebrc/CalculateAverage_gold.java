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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
        // Page size is 16KB. MMap will allocate a page of mem on each reach. Therefore
        // this is the max parallel throughput we get
        // without duplicating mmap calls.
        final Long macPageSize = 16384L * 1280;

        // use a Map since insertion/contains is going to happen K times. Laster we need
        // ordering which is NlgN
        final Stream<ByteBuffer> fileChunks = FastFiles.readMMapChunks(Paths.get(FILE), macPageSize);
        final Optional<Map<String, MeasurementAggregation>> aggregates = fileChunks
                .parallel()
                .map(CalculateAverage_gold::toMeasureStream)
                .map(CalculateAverage_gold::streamToAggregate)
                .collect(Collectors.reducing(CalculateAverage_gold::combineMaps));

        final Map<String, MeasurementAggregation> aggregationsSorted = new TreeMap<>(aggregates.orElseThrow());
        System.out.println(aggregationsSorted);
    }

    private static Stream<Measurement> toMeasureStream(final ByteBuffer buffer) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new MeasurementIterator(buffer), Spliterator.IMMUTABLE),
                false);
    }

    private static Map<String, MeasurementAggregation> streamToAggregate(final Stream<Measurement> measures) {
        final Map<String, MeasurementAggregation> aggregates = new HashMap<>();

        measures.forEach(measure -> {
            if (aggregates.containsKey(measure.station)) {
                MeasurementAggregation aggregation = aggregates.get(measure.station);
                aggregation.appendValue(measure.value);
                // don't need to insert since we are using the same object reference
                return;
            }
            MeasurementAggregation aggregation = new MeasurementAggregation(measure.value);
            aggregates.put(measure.station, aggregation);
        });

        return aggregates;
    }

    private static Map<String, MeasurementAggregation> combineMaps(Map<String, MeasurementAggregation> map1,
                                                                   Map<String, MeasurementAggregation> map2) {
        for (var entry : map2.entrySet()) {
            map1.merge(entry.getKey(), entry.getValue(), MeasurementAggregation::combine);
        }

        return map1;
    }

    private static class MeasurementIterator implements Iterator<Measurement> {
        private final ByteBuffer buffer;
        private int currentIdx;
        private int maxSize;

        public MeasurementIterator(final ByteBuffer buffer) {
            this.buffer = buffer;
            this.currentIdx = 0;
            this.maxSize = buffer.limit();
        }

        @Override
        public boolean hasNext() {
            return this.currentIdx < this.maxSize;
        }

        @Override
        public Measurement next() {
            final byte[] bytes = new byte[48];
            String key = null;
            String value = null;

            int currentLineIdx = 0;
            int separatorIdx = 0;
            while (currentLineIdx + this.currentIdx < this.maxSize) {
                final char currentChar = (char) buffer.get(currentLineIdx + this.currentIdx);
                bytes[currentLineIdx] = (byte) currentChar;

                if (currentChar == ';') {
                    key = new String(bytes, 0, currentLineIdx);
                    separatorIdx = currentLineIdx;
                }

                currentLineIdx++;
                if (currentChar == '\n') {
                    value = new String(bytes, separatorIdx + 1, currentLineIdx - separatorIdx);
                    break;
                }
            }

            final Measurement measure = new Measurement(key, Double.parseDouble(value));
            this.currentIdx += currentLineIdx;

            return measure;
        }

    }
}

// Attempt #1: Just parrallelize with HashMap and TreeMap
// Attempt #2: parrallel with TreeMap only.
// Attempt #3: Using Java NIO to create a memory mapped file when reading the
// file by mapping 64mB chunks.

// TODO: Attemp #4: Make smaller file chunks that align with my page size
// TODO: Attempt #5: Create chunks based on the number of available processors
// TODO: Attemp #6: instead of parralle() try virtual threads