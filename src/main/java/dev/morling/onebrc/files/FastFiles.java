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
package dev.morling.onebrc.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FastFiles {

    /**
     * Returns a stream of file chunk ByteBuffers of the file.
     * 
     * @param filePath
     * @return
     */
    public static Stream<ByteBuffer> readMMapChunks(Path filePath, final Long chunk_size) {
        final FileChannel fc;
        try {
            fc = FileChannel.open(filePath, StandardOpenOption.READ);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(new MMapFileIterator(fc, chunk_size), Spliterator.IMMUTABLE),
                        false)
                .onClose(() -> {
                    try {
                        fc.close();
                    }
                    catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

    }

    /**
     * Returns a Stream of Strings for each line in the byte buffer
     * 
     * @param buffer
     * @return
     */
    public static Stream<String> lines(final ByteBuffer buffer) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new ByteBufferLineIterator(buffer), Spliterator.IMMUTABLE),
                false);
    }

    /**
     * A iterator which iterates over a file and returns MMaped chuncks of that file
     * which align on a file line boundary.
     */
    private static class MMapFileIterator implements Iterator<ByteBuffer> {
        private static final long CHUNK_SIZE = 1024 * 1024 * 64L; // 64MB chuncks

        private final long size;
        private long currentIdx;
        private final FileChannel fc;
        private final long chunk_size;

        public MMapFileIterator(final FileChannel fc, final Long chunk_size) {
            this.fc = fc;
            try {
                this.size = fc.size();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            this.currentIdx = 0;
            this.chunk_size = (chunk_size != null) ? chunk_size : CHUNK_SIZE;
        }

        @Override
        public boolean hasNext() {
            return currentIdx < size;
        }

        /**
         * Returns the Next MMap File Chunks.
         */
        @Override
        public ByteBuffer next() {
            try {
                final long chunkSize = Math.min(this.chunk_size, this.size - this.currentIdx);
                final MappedByteBuffer mappedByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, currentIdx, chunkSize);

                final long chunkSizeUpdated = this.alignToLine(mappedByteBuffer);
                currentIdx += chunkSizeUpdated;

                return mappedByteBuffer;
            }
            catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        /**
         * Re-algins the byte buffer to end on a new line. This way the consumer doesn't
         * get a
         * byte buffer which starts or ends in the middle of a line.
         * 
         * @param buffer -- ByteBuffer of file contents
         * @returns The updated size of the mmap file
         */
        private int alignToLine(final ByteBuffer buffer) {
            // find last new line character
            int chunkEnd = buffer.limit() - 1;
            while (true) {
                final char currentChar = (char) buffer.get(chunkEnd);
                if (currentChar == '\n' || currentChar == '\r') {
                    break;
                }

                chunkEnd--;
            }

            buffer.limit(chunkEnd + 1); // + 1 since its 0 based size
            return chunkEnd + 1;
        }
    }

    /**
     * Iterator to take a byte buffer and return the next line (until \n)
     */
    private static class ByteBufferLineIterator implements Iterator<String> {
        private final ByteBuffer buffer;
        private int currentIdx;
        private int maxSize;

        public ByteBufferLineIterator(final ByteBuffer buffer) {
            this.buffer = buffer;
            this.currentIdx = 0;
            this.maxSize = buffer.limit();
        }

        @Override
        public boolean hasNext() {
            return this.currentIdx < this.maxSize;
        }

        @Override
        public String next() {
            final byte[] bytes = new byte[48];

            int currentLineIdx = 0;
            while (currentLineIdx + this.currentIdx < this.maxSize) {
                final char currentChar = (char) buffer.get(currentLineIdx + this.currentIdx);
                bytes[currentLineIdx] = (byte) currentChar;
                currentLineIdx++;
                if (currentChar == '\n') {
                    break;
                }
            }

            final String line = new String(bytes, 0, currentLineIdx);
            this.currentIdx += currentLineIdx;

            return line;
        }

    }
}
