package com.squadra.platform.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LayerPlacementTest {
    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/squadra/platform");
    private static final Set<String> LAYERS = Set.of("api", "application", "domain", "infra");

    @Test
    void productionFilesAreNotLooseInsideLayerRoots() throws IOException {
        for (String layer : LAYERS) {
            Path layerRoot = SOURCE_ROOT.resolve(layer);
            if (!Files.exists(layerRoot)) {
                continue;
            }

            try (var files = Files.list(layerRoot)) {
                var looseJavaFiles = files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .map(Path::toString)
                        .toList();

                assertTrue(looseJavaFiles.isEmpty(), () -> "Loose files in " + layerRoot + ": " + looseJavaFiles);
            }
        }
    }
}
