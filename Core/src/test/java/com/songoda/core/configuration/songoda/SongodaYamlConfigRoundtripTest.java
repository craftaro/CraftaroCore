package com.songoda.core.configuration.songoda;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SongodaYamlConfigRoundtripTest {
    Path cfg;

    @BeforeEach
    void setUp() throws IOException {
        Path path = Files.createTempFile("SongodaYamlConfigTest", "yml");
        File file = path.toFile();
        file.deleteOnExit();

        this.cfg = path;
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(this.cfg);
    }

    @Test
    void roundtripTest() throws IOException {
        Files.write(this.cfg, ("# Don't touch this – it's used to track the version of the config.\n" +
                "version: 1\n" +
                "messages:\n" +
                "  # This message is shown when the 'foo' command succeeds.\n" +
                "  fooSuccess: Remastered success value\n" +
                "# This is the range of the 'foo' command\n").getBytes());

        SongodaYamlConfig cfg = new SongodaYamlConfig(this.cfg.toFile())
                .withVersion(3);

        ConfigEntry cmdFooSuccess = new ConfigEntry(cfg, "command.foo.success", "Default success value")
                .withComment("This message is shown when the 'foo' command succeeds.")
                .withUpgradeStep(1, "messages.fooSuccess");
        ConfigEntry range = new ConfigEntry(cfg, "range")
                .withComment("This is the range of the 'foo' command")
                .withUpgradeStep(1, null, o -> {
                    if (o == null) {
                        return 10;
                    }

                    return o;
                })
                .withUpgradeStep(2, null, o -> o + " blocks");
        ConfigEntry incrementer = new ConfigEntry(cfg, "incrementer", 0)
                .withComment("This is the incrementer of the 'foo' command")
                .withUpgradeStep(1, null, o -> {
                    if (o == null) {
                        return null;
                    }

                    return (int) o + 1;
                })
                .withUpgradeStep(3, null, (o) -> "text");

        assertTrue(cfg.init());

        assertNull(cfg.get("messages.fooSuccess"));
        assertEquals("Remastered success value", cfg.get("command.foo.success"));
        assertEquals("Remastered success value", cmdFooSuccess.get());
        assertTrue(cmdFooSuccess.has());

        assertTrue(range.has());
        assertEquals(cfg.get("range"), range.get());

        assertTrue(incrementer.has());
        assertEquals(cfg.get("incrementer"), incrementer.get());

        assertEquals("# Don't touch this – it's used to track the version of the config.\n" +
                "version: 3\n" +
                "command:\n" +
                "  foo:\n" +
                "    # This message is shown when the 'foo' command succeeds.\n" +
                "    success: Remastered success value\n" +
                "# This is the range of the 'foo' command\n" +
                "range: 10 blocks\n" +
                "# This is the incrementer of the 'foo' command\n" +
                "incrementer: 0\n", new String(Files.readAllBytes(this.cfg)));
    }
}
