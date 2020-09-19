package cn.kuncb.threads;


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * ByteArrayOutputStream
 * 目的:提升性能但线程不安全,线程安全由开发人员控制
 */
public class ByteArrayOutputStreamT extends OutputStream {

    private static final long serialVersionUID = 999578090392953855L;
    /* 缓冲区 */
    private byte[] mBuffer;
    /* 缓冲区已使用部份的大小 */
    private int mPostion;
    /* 缓冲区大小 */
    private int mCapacity;

    /* 2 gigabyte - 8 */
    private static final int MAXALLOCSIZE = 2147483639;


    public ByteArrayOutputStreamT() {
        init(256);
    }

    public ByteArrayOutputStreamT(int memorySize) {
        init(memorySize);
    }

    /**
     * 初始化
     *
     * @param memorySize 初始化时占用的内存大小
     */
    private final void init(int memorySize) {
        int initSize = (memorySize < 256 || memorySize > MAXALLOCSIZE) ? 256 : memorySize;    /* initial default buffer size */
        this.mBuffer = new byte[initSize];
        this.mPostion = 0;
        this.mCapacity = initSize;
    }

    /**
     * 获取当前分配的内存大小
     */
    @Contract(pure = true)
    public final int capacity() {
        return this.mCapacity;
    }

    /**
     * 获取当前已使用的空间
     */
    @Contract(pure = true)
    public final int size() {
        return this.mPostion;
    }

    /**
     * 根据needed和this.capacity检查是否需要重新分配内存
     * 只有needed超过this.capacity时才会重新分配内存,否则仅做检查
     * 新分配的内存大小为2*this.capacity
     * 最大允许使用的内存大小为2GB
     *
     * @param requestSize 要添加的字符串长度
     */
    private final void ensureCapacity(int requestSize) {
        int newSize;
        if (requestSize < 1 || requestSize >= (MAXALLOCSIZE - this.mPostion)) /*申请的内存大小超过了最大内容大小*/
            throw new IllegalArgumentException(String.format("Cannot enlarge string buffer containing %d bytes by %d more bytes.", this.mPostion, requestSize));
        requestSize += this.mPostion + 1;
        if (requestSize <= this.mCapacity)
            return;
        newSize = (this.mCapacity << 1);    //(this.mCapacity<<1)表示this.mCapacity*2
        while (requestSize > newSize)
            newSize *= 2;
        if (newSize > MAXALLOCSIZE)
            newSize = MAXALLOCSIZE;

        byte[] old = this.mBuffer;
        this.mBuffer = new byte[newSize];
        this.mCapacity = newSize;
        System.arraycopy(old, 0, this.mBuffer, 0, this.mPostion);
    }

    public void write(int val) {
        this.ensureCapacity(this.mPostion + 1);
        this.mBuffer[this.mPostion++] = (byte) val;
    }

    public void write(byte[] val, int postion, int len) {
        if (postion >= 0 && postion <= val.length && len >= 0 && postion + len - val.length <= 0) {
            this.ensureCapacity(this.mPostion + len);
            System.arraycopy(val, postion, this.mBuffer, this.mPostion, len);
            this.mPostion += len;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public void writeTo(@NotNull OutputStream outputStream) throws IOException {
        outputStream.write(this.mBuffer, 0, this.mPostion);
    }

    public final void reset() {
        this.mPostion = 0;
    }

    public final byte[] toByteArray() {
        return this.mBuffer;
    }


    @NotNull
    @Contract(" -> new")
    public final String toString() {
        return new String(this.mBuffer, 0, this.mPostion);
    }

    @NotNull
    @Contract("_ -> new")
    public final String toString(String charSet) throws UnsupportedEncodingException {
        return new String(this.mBuffer, 0, this.mPostion, charSet);
    }


    public void close() throws IOException {
    }
}
