package com.veritas.nlp.service;

import com.google.common.io.Resources;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * An implementation of {@link ConfigurationSourceProvider} that reads the configuration from the
 * class path or the local file system (file system takes priority).
 */
class FileOrClassPathConfigurationSourceProvider implements ConfigurationSourceProvider {
    @Override
    public InputStream open(String path) throws IOException {
        final File file = new File(path);

        if (!file.exists()) {
            try {
                return Resources.getResource(path).openStream();
            } catch (Exception e){

            }
            throw new FileNotFoundException("File " + file + " not found");
        }

        return new FileInputStream(file);
    }
}