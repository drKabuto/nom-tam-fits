package nom.tam.fits.compress;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 1996 - 2015 nom-tam-fits
 * %%
 * This is free and unencumbered software released into the public domain.
 * 
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * #L%
 */

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CloseIS extends FilterInputStream {

    private static final int COPY_BUFFER_SIZE = 64 * 1024;

    private InputStream output;

    private OutputStream input;

    private String errorText;

    private IOException exception;

    private final Thread stdError;

    private final Thread copier;

    private final Process proc;

    public CloseIS(Process proc, final InputStream compressed) {
        super(new BufferedInputStream(proc.getInputStream(), CompressionManager.ONE_MEGABYTE));
        this.proc = proc;
        final InputStream error = proc.getErrorStream();
        this.output = proc.getInputStream();
        this.input = proc.getOutputStream();
        stdError = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[COPY_BUFFER_SIZE];
                    int len;
                    while ((len = error.read(buffer, 0, buffer.length)) >= 0) {
                        bytes.write(buffer, 0, len);
                    }
                    error.close();
                    errorText = new String(bytes.toByteArray());
                } catch (IOException e) {
                    exception = e;
                }
            }
        });
        // Now copy everything in a separate thread.
        copier = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[COPY_BUFFER_SIZE];
                    int len;
                    while ((len = compressed.read(buffer, 0, buffer.length)) >= 0) {
                        input.write(buffer, 0, len);
                    }
                    input.close();
                } catch (IOException e) {
                    exception = e;
                }
                try {
                    compressed.close();
                } catch (IOException e) {
                    if (exception == null) {
                        exception = e;
                    }
                }
            }
        });
        stdError.start();
        copier.start();
    }

    @Override
    public int read() throws IOException {
        int result = 0;
        try {
            return (result = super.read());
        } catch (IOException e) {
            throw e;
        } finally {
            handledOccuredException(result);
        }
    }

    private void handledOccuredException(int result) throws IOException {
        int exitValue = 0;
        if (result < 0) {
            try {
                stdError.join();
                copier.join();
                exitValue = proc.exitValue();
            } catch (Exception e) {
                "".toString();
                // ignore
            }
        }
        if (exception != null || exitValue != 0) {
            if (errorText != null && !errorText.trim().isEmpty()) {
                throw new IOException(errorText, exception);
            } else {
                if (exception == null) {
                    throw new IOException("exit value was " + exitValue);
                } else {
                    throw exception;
                }
            }
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = 0;
        try {
            return (result = super.read(b, off, len));
        } catch (IOException e) {
            throw e;
        } finally {
            handledOccuredException(result);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.input.close();
        this.output.close();
    }

}
