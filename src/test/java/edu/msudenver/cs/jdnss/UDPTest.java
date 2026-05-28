package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.mockito.Mockito.mock;

public class UDPTest {

    private static class ExitCalled extends RuntimeException {
    }

    private static class TestUDP extends UDP {
        private final byte[] payload;
        private final boolean failFirstReceive;
        private int receiveCalls;
        private int submittedTasks;
        private boolean exited;

        TestUDP(final byte[] payload, final boolean failFirstReceive) {
            this.payload = payload;
            this.failFirstReceive = failFirstReceive;
            this.dsocket = mock(DatagramSocket.class);
        }

        @Override
        ExecutorService createThreadPool() {
            return mock(ExecutorService.class);
        }

        @Override
        void receivePacket(final DatagramPacket packet) throws IOException {
            receiveCalls++;
            if (failFirstReceive && receiveCalls == 1) {
                throw new IOException("first receive failed");
            }

            System.arraycopy(payload, 0, packet.getData(), 0, payload.length);
            packet.setLength(payload.length);
            packet.setAddress(InetAddress.getLoopbackAddress());
            packet.setPort(53);
        }

        @Override
        Future<?> submitPacketTask(final ExecutorService pool, final DatagramPacket packet) {
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
    public void packetSizeIsStandardForUdp() {
        UDP udp = new UDP();
        Assert.assertEquals(512, udp.packetSize());
    }

    @Test
    public void runInOnceModeSubmitsTaskAndExits() throws Exception {
        setJargField("once", true);

        TestUDP udp = new TestUDP(new byte[] {1, 2, 3, 4}, false);

        Assert.assertThrows(ExitCalled.class, udp::run);
        Assert.assertEquals(1, udp.submittedTasks);
        Assert.assertTrue(udp.exited);
    }

    @Test
    public void runContinuesAfterReceiveExceptionThenExitsOnSuccess() throws Exception {
        setJargField("once", true);

        TestUDP udp = new TestUDP(new byte[] {5, 6, 7}, true);

        Assert.assertThrows(ExitCalled.class, udp::run);
        Assert.assertTrue(udp.receiveCalls >= 2);
        Assert.assertEquals(1, udp.submittedTasks);
        Assert.assertTrue(udp.exited);
    }

    private static void setJargField(final String fieldName, final Object value) throws Exception {
        Field field = jdnssArgs.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(JDNSS.jargs, value);
    }
}
