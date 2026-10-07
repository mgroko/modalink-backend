package org.mgroko.backend;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.io.IOException;
import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
public class Application {

    static void limpiarDllsWebpHuerfanos() {
        Path tmp = Path.of(System.getProperty("java.io.tmpdir"));
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(tmp, "*webp-imageio.dll")) {
            for (Path p : ds) {
                try {
                    Files.delete(p);
                } catch (IOException ignorado) {
                    // en uso por otra JVM
                }
            }
        } catch (IOException ignorado) {
            // no se pudo listar el directorio temporal
        }
    }

    public static void main(String[] args) {
        limpiarDllsWebpHuerfanos();
        SpringApplication.run(Application.class, args);
    }
}