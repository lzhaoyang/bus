/*
 * The MIT License
 *
 * Copyright (c) 2017 aoju.org All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.aoju.bus.core.io.segment;

import org.aoju.bus.core.utils.IoUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * 由字节数组段组成的不可变字节字符串。该类的存在是为了实现
 * 缓冲区的有效快照。它被实现为一个段数组，加上一个目录
 * 两个半部分，描述段如何组成这个字节字符串。
 *
 * @author Kimi Liu
 * @version 3.6.6
 * @since JDK 1.8+
 */
public class Segmented extends ByteString {

    final transient byte[][] segments;
    final transient int[] directory;

    public Segmented(Buffer buffer, int byteCount) {
        super(null);
        IoUtils.checkOffsetAndCount(buffer.size, 0, byteCount);

        // Walk through the buffer to count how many segments we'll need.
        int offset = 0;
        int segmentCount = 0;
        for (Segment s = buffer.head; offset < byteCount; s = s.next) {
            if (s.limit == s.pos) {
                throw new AssertionError("s.limit == s.pos"); // Empty segment. This should not happen!
            }
            offset += s.limit - s.pos;
            segmentCount++;
        }

        // Walk through the buffer again to assign segments and build the directory.
        this.segments = new byte[segmentCount][];
        this.directory = new int[segmentCount * 2];
        offset = 0;
        segmentCount = 0;
        for (Segment s = buffer.head; offset < byteCount; s = s.next) {
            segments[segmentCount] = s.data;
            offset += s.limit - s.pos;
            if (offset > byteCount) {
                offset = byteCount; // Despite sharing more bytes, only report having up to byteCount.
            }
            directory[segmentCount] = offset;
            directory[segmentCount + segments.length] = s.pos;
            s.shared = true;
            segmentCount++;
        }
    }

    @Override
    public String utf8() {
        return toByteString().utf8();
    }

    @Override
    public String string(Charset charset) {
        return toByteString().string(charset);
    }

    @Override
    public String base64() {
        return toByteString().base64();
    }

    @Override
    public String hex() {
        return toByteString().hex();
    }

    @Override
    public ByteString toAsciiLowercase() {
        return toByteString().toAsciiLowercase();
    }

    @Override
    public ByteString toAsciiUppercase() {
        return toByteString().toAsciiUppercase();
    }

    @Override
    public ByteString md5() {
        return toByteString().md5();
    }

    @Override
    public ByteString sha1() {
        return toByteString().sha1();
    }

    @Override
    public ByteString sha256() {
        return toByteString().sha256();
    }

    @Override
    public ByteString hmacSha1(ByteString key) {
        return toByteString().hmacSha1(key);
    }

    @Override
    public ByteString hmacSha256(ByteString key) {
        return toByteString().hmacSha256(key);
    }

    @Override
    public String base64Url() {
        return toByteString().base64Url();
    }

    @Override
    public ByteString substring(int beginIndex) {
        return toByteString().substring(beginIndex);
    }

    @Override
    public ByteString substring(int beginIndex, int endIndex) {
        return toByteString().substring(beginIndex, endIndex);
    }

    @Override
    public byte getByte(int pos) {
        IoUtils.checkOffsetAndCount(directory[segments.length - 1], pos, 1);
        int segment = segment(pos);
        int segmentOffset = segment == 0 ? 0 : directory[segment - 1];
        int segmentPos = directory[segment + segments.length];
        return segments[segment][pos - segmentOffset + segmentPos];
    }

    /**
     * Returns the index of the segment that contains the byte at {@code pos}.
     */
    private int segment(int pos) {
        // Search for (pos + 1) instead of (pos) because the directory holds sizes, not indexes.
        int i = Arrays.binarySearch(directory, 0, segments.length, pos + 1);
        return i >= 0 ? i : ~i; // If i is negative, bitflip to get the insert position.
    }

    @Override
    public int size() {
        return directory[segments.length - 1];
    }

    @Override
    public byte[] toByteArray() {
        byte[] result = new byte[directory[segments.length - 1]];
        int segmentOffset = 0;
        for (int s = 0, segmentCount = segments.length; s < segmentCount; s++) {
            int segmentPos = directory[segmentCount + s];
            int nextSegmentOffset = directory[s];
            System.arraycopy(segments[s], segmentPos, result, segmentOffset,
                    nextSegmentOffset - segmentOffset);
            segmentOffset = nextSegmentOffset;
        }
        return result;
    }

    @Override
    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(toByteArray()).asReadOnlyBuffer();
    }

    @Override
    public void write(OutputStream out) throws IOException {
        if (out == null) throw new IllegalArgumentException("out == null");
        int segmentOffset = 0;
        for (int s = 0, segmentCount = segments.length; s < segmentCount; s++) {
            int segmentPos = directory[segmentCount + s];
            int nextSegmentOffset = directory[s];
            out.write(segments[s], segmentPos, nextSegmentOffset - segmentOffset);
            segmentOffset = nextSegmentOffset;
        }
    }

    @Override
    public void write(Buffer buffer) {
        int segmentOffset = 0;
        for (int s = 0, segmentCount = segments.length; s < segmentCount; s++) {
            int segmentPos = directory[segmentCount + s];
            int nextSegmentOffset = directory[s];
            Segment segment = new Segment(segments[s], segmentPos,
                    segmentPos + nextSegmentOffset - segmentOffset, true, false);
            if (buffer.head == null) {
                buffer.head = segment.next = segment.prev = segment;
            } else {
                buffer.head.prev.push(segment);
            }
            segmentOffset = nextSegmentOffset;
        }
        buffer.size += segmentOffset;
    }

    @Override
    public boolean rangeEquals(
            int offset, ByteString other, int otherOffset, int byteCount) {
        if (offset < 0 || offset > size() - byteCount) return false;
        // Go segment-by-segment through this, passing arrays to other's rangeEquals().
        for (int s = segment(offset); byteCount > 0; s++) {
            int segmentOffset = s == 0 ? 0 : directory[s - 1];
            int segmentSize = directory[s] - segmentOffset;
            int stepSize = Math.min(byteCount, segmentOffset + segmentSize - offset);
            int segmentPos = directory[segments.length + s];
            int arrayOffset = offset - segmentOffset + segmentPos;
            if (!other.rangeEquals(otherOffset, segments[s], arrayOffset, stepSize)) return false;
            offset += stepSize;
            otherOffset += stepSize;
            byteCount -= stepSize;
        }
        return true;
    }

    @Override
    public boolean rangeEquals(int offset, byte[] other, int otherOffset, int byteCount) {
        if (offset < 0 || offset > size() - byteCount
                || otherOffset < 0 || otherOffset > other.length - byteCount) {
            return false;
        }
        // Go segment-by-segment through this, comparing ranges of arrays.
        for (int s = segment(offset); byteCount > 0; s++) {
            int segmentOffset = s == 0 ? 0 : directory[s - 1];
            int segmentSize = directory[s] - segmentOffset;
            int stepSize = Math.min(byteCount, segmentOffset + segmentSize - offset);
            int segmentPos = directory[segments.length + s];
            int arrayOffset = offset - segmentOffset + segmentPos;
            if (!IoUtils.arrayRangeEquals(segments[s], arrayOffset, other, otherOffset, stepSize)) return false;
            offset += stepSize;
            otherOffset += stepSize;
            byteCount -= stepSize;
        }
        return true;
    }

    @Override
    public int indexOf(byte[] other, int fromIndex) {
        return toByteString().indexOf(other, fromIndex);
    }

    @Override
    public int lastIndexOf(byte[] other, int fromIndex) {
        return toByteString().lastIndexOf(other, fromIndex);
    }

    /**
     * Returns a copy as a non-segmented byte string.
     */
    private ByteString toByteString() {
        return new ByteString(toByteArray());
    }

    @Override
    public byte[] internalArray() {
        return toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        return o instanceof ByteString
                && ((ByteString) o).size() == size()
                && rangeEquals(0, ((ByteString) o), 0, size());
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result != 0) return result;

        // Equivalent to Arrays.hashCode(toByteArray()).
        result = 1;
        int segmentOffset = 0;
        for (int s = 0, segmentCount = segments.length; s < segmentCount; s++) {
            byte[] segment = segments[s];
            int segmentPos = directory[segmentCount + s];
            int nextSegmentOffset = directory[s];
            int segmentSize = nextSegmentOffset - segmentOffset;
            for (int i = segmentPos, limit = segmentPos + segmentSize; i < limit; i++) {
                result = (31 * result) + segment[i];
            }
            segmentOffset = nextSegmentOffset;
        }
        return (hashCode = result);
    }

    @Override
    public String toString() {
        return toByteString().toString();
    }

    private Object writeReplace() {
        return toByteString();
    }

}