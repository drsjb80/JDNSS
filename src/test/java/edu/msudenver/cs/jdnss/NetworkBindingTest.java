package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class NetworkBindingTest {

    @Test
    public void fromPartsParsesHostAndPort() {
        NetworkBinding binding = NetworkBinding.fromParts(new String[] {"TCP", "127.0.0.1", "53"});

        Assert.assertEquals("127.0.0.1", binding.getHost());
        Assert.assertEquals(53, binding.getPort());
    }

    @Test
    public void fromPartsRejectsInvalidPort() {
        Assert.assertThrows(NumberFormatException.class,
                () -> NetworkBinding.fromParts(new String[] {"TCP", "127.0.0.1", "not-a-port"}));
    }

    @Test
    public void fromPartsRequiresHostAndPortEntries() {
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> NetworkBinding.fromParts(new String[] {"TCP"}));
    }
}
