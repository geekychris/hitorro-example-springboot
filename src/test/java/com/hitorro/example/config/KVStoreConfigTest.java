package com.hitorro.example.config;

import com.hitorro.kvstore.KVStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The store's path may sit under directories that don't exist yet (the
 * default is ${HT_HOME}/data/kvstore); RocksDB creates only the last one.
 */
class KVStoreConfigTest {

    @Test
    void createsMissingParentDirectories(@TempDir Path tmp) throws Exception {
        Path store = tmp.resolve("home/data/kvstore");
        KVStoreConfig config = new KVStoreConfig();
        ReflectionTestUtils.setField(config, "kvStorePath", store.toString());
        ReflectionTestUtils.setField(config, "compressionType", "SNAPPY");
        ReflectionTestUtils.setField(config, "writeBufferSize", 4L * 1024 * 1024);
        ReflectionTestUtils.setField(config, "maxOpenFiles", 100);

        KVStore kv = config.documentStore();
        try {
            assertThat(Files.isDirectory(store)).isTrue();
        } finally {
            kv.close();
        }
    }
}
