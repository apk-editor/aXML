package com.apk.axml.ARSCUtils;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

public class PositionInputStream extends FilterInputStream {
    protected long position = 0;
    private long markedPosition = 0;

    public PositionInputStream(InputStream inputStream) {
        super(inputStream);
    }
    public PositionInputStream(InputStream inputStream, long position) {
        super(inputStream);
        this.position = position;
    }

    public synchronized long getPosition() {
        return position;
    }

    @Override
    public synchronized int read() throws IOException {
        int p = in.read();
        if (p != -1)
            position++;
        return p;
    }
    
    @Override
    public synchronized int read(byte[] b) throws IOException {
        int p = in.read(b);
        if (p > 0)
            position += p;        
        return p;
    }
    
    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        int p = in.read(b, off, len);
        if (p > 0)
            position += p;        
        return p;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        long p = in.skip(n);
        if (p > 0)
            position += p;
        return p;
    }

    public synchronized void seek(long n) throws IOException {
        in.reset();
        position=0;
        long p = in.skip(n);
        if (p > 0)
            position += p;
    }
    
    @Override
    public synchronized void reset() throws IOException {
        in.reset();
        position = markedPosition;
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
        markedPosition = position;
    }
}