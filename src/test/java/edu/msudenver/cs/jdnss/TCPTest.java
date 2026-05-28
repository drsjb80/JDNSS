package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TCPTest {

    private static class ExitCalled extends RuntimeException {
    }

    private static class TestTCP extends TCP {
        private final ServerSocket serverSocket;
        private int submittedTasks;
        private boolean exited;

        TestTCP(final String[] parts, final ServerSocket serverSocket) {
            super(parts);
            this.serverSocket = serverSocket;
        }

        @Override
        ServerSocket createServerSocket() {
            return serverSocket;
        }

        @Override
        ExecutorService createThreadPool() {
            return mock(ExecutorService.class);
        }

        @Override
        Future<?> submitSocketTask(final ExecutorService pool, final Socket socket) {
            submittedTasks++;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        void exitProcess(final int statusCode) {
            exited = true;
            throw new ExitCalled();
        }
    }

    @Test
    public void runReturnsWhenAcceptThrowsIOException() throws Exception {
        setJargField("once", false);

        ServerSocket serverSocket = mock(ServerSocket.class);
        when(serverSocket.accept()).thenThrow(new IOException("accept-failed"));

        TestTCP tcp = new TestTCP(new String[] {"TCP", "127.0.0.1", "53"}, serverSocket);
        tcp.run();

        Assert.assertEquals(0, tcp.submittedTasks);
        Assert.assertFalse(tcp.exited);
    }

    @Test
    public void runInOnceModeSubmitsTaskAndExits() throws Exception {
        setJargField("once", true);

        ServerSocket serverSocket = mock(ServerSocket.class);
        Socket socket = mock(Socket.class);
        when(serverSocket.accept()).thenReturn(socket);

        TestTCP tcp = new TestTCP(new String[] {"TCP", "127.0.0.1", "53"}, serverSocket);

        Assert.assertThrows(ExitCalled.class, tcp::run);
        Assert.assertEquals(1, tcp.submittedTasks);
        Assert.assertTrue(tcp.exited);
    }

    private static void setJargField(final String fieldName, final Object value) throws Exception {
        Field field = jdnssArgs.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(JDNSS.jargs, value);
    }
}
