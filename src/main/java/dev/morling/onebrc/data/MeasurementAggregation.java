
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
package dev.morling.onebrc.data;

public class MeasurementAggregation {
    private double min;
    private double max;
    private double sum;
    private long count;

    public MeasurementAggregation(final double min, final double max, final double sum, final long count) {
        this.min = min;
        this.max = max;
        this.sum = sum;
        this.count = count;
    }

    public MeasurementAggregation(final double value) {
        this.min = value;
        this.max = value;
        this.sum = value;
        this.count = 1;
    }

    /**
     * Adds a value to the aggregation. 
     * @param value -- a measurement
     */
    public void appendValue(double value) {
        this.min = Math.min(this.min, value);
        this.max = Math.max(this.max, value);
        this.sum += value;
        this.count++;
    }

    public MeasurementAggregation combine(MeasurementAggregation agg) {
        this.min = Math.min(this.min, agg.min);
        this.max = Math.max(this.max, agg.max);
        this.sum += agg.sum;
        this.count += agg.count;
        return this;
    }

    private double getMean() {
        return this.sum / this.count;
    }

    public String toString() {
        return round(this.min) + "/" + round(this.getMean()) + "/" + round(this.max);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
