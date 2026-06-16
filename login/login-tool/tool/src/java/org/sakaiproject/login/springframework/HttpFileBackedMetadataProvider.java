/**
 * Copyright (c) 2003-2026 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.login.springframework;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import lombok.extern.slf4j.Slf4j;

import org.opensaml.saml2.metadata.provider.AbstractReloadingMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.XMLObject;
import org.w3c.dom.Document;

@Slf4j
public class HttpFileBackedMetadataProvider extends AbstractReloadingMetadataProvider {

    private final URI metadataUri;
    private final int requestTimeout;
    private final Path backupFile;
    private final HttpClient httpClient;

    private String cachedMetadataETag;
    private String cachedMetadataLastModified;

    public HttpFileBackedMetadataProvider(String metadataURL, int requestTimeout, String backupFilePath)
            throws MetadataProviderException {
        try {
            this.metadataUri = new URI(metadataURL);
        } catch (URISyntaxException e) {
            throw new MetadataProviderException("Illegal URL syntax", e);
        }
        this.requestTimeout = requestTimeout;
        this.backupFile = Path.of(backupFilePath);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(requestTimeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    protected String getMetadataIdentifier() {
        return metadataUri.toASCIIString();
    }

    @Override
    protected void doInitialization() throws MetadataProviderException {
        try {
            validateBackupFile();
        } catch (MetadataProviderException e) {
            if (isFailFastInitialization()) {
                throw e;
            }
            log.warn("Metadata backup file path was invalid, continuing without known good backup file", e);
        }

        super.doInitialization();
    }

    @Override
    protected byte[] fetchMetadata() throws MetadataProviderException {
        try {
            HttpRequest request = buildRequest();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 304) {
                log.debug("Metadata document from '{}' has not changed since last retrieval", metadataUri);
                return null;
            }

            if (response.statusCode() != 200) {
                throw new MetadataProviderException("Non-ok status code " + response.statusCode()
                        + " returned from remote metadata source " + metadataUri);
            }

            cachedMetadataETag = response.headers().firstValue("ETag").orElse(cachedMetadataETag);
            cachedMetadataLastModified = response.headers().firstValue("Last-Modified").orElse(cachedMetadataLastModified);

            return decodeResponseBody(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fetchBackupMetadata(e);
        } catch (Exception e) {
            return fetchBackupMetadata(e);
        }
    }

    @Override
    protected void postProcessMetadata(byte[] metadataBytes, Document metadataDom, XMLObject metadata)
            throws MetadataProviderException {
        try {
            validateBackupFile();
            Files.write(backupFile, metadataBytes);
        } catch (IOException | MetadataProviderException e) {
            log.error("Unable to write metadata to backup file: {}", backupFile.toAbsolutePath(), e);
        } finally {
            super.postProcessMetadata(metadataBytes, metadataDom, metadata);
        }
    }

    private HttpRequest buildRequest() {
        HttpRequest.Builder builder = HttpRequest.newBuilder(metadataUri)
                .timeout(Duration.ofMillis(requestTimeout))
                .header("Accept-Encoding", "gzip,deflate")
                .GET();

        if (cachedMetadataETag != null) {
            builder.header("If-None-Match", cachedMetadataETag);
        }
        if (cachedMetadataLastModified != null) {
            builder.header("If-Modified-Since", cachedMetadataLastModified);
        }

        return builder.build();
    }

    private byte[] decodeResponseBody(HttpResponse<byte[]> response) throws IOException {
        byte[] body = response.body();
        String contentEncoding = response.headers().firstValue("Content-Encoding").orElse("");

        if ("gzip".equalsIgnoreCase(contentEncoding)) {
            try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(body))) {
                return input.readAllBytes();
            }
        }

        if ("deflate".equalsIgnoreCase(contentEncoding)) {
            try (InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(body))) {
                return input.readAllBytes();
            }
        }

        return body;
    }

    private byte[] fetchBackupMetadata(Exception cause) throws MetadataProviderException {
        if (Files.isRegularFile(backupFile) && isNonEmpty(backupFile)) {
            log.warn("Problem reading metadata from remote source, processing existing backup file: {}",
                    backupFile.toAbsolutePath(), cause);
            try {
                return Files.readAllBytes(backupFile);
            } catch (IOException e) {
                throw new MetadataProviderException("Unable to retrieve metadata from backup file "
                        + backupFile.toAbsolutePath(), e);
            }
        }

        throw new MetadataProviderException("Unable to read metadata from remote server and backup does not exist", cause);
    }

    private void validateBackupFile() throws MetadataProviderException {
        try {
            Path parent = backupFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.notExists(backupFile)) {
                Files.createFile(backupFile);
            }
            if (!Files.isRegularFile(backupFile)) {
                throw new MetadataProviderException("Filepath " + backupFile.toAbsolutePath()
                        + " is a directory and may not be used as a backing metadata file");
            }
            if (!Files.isReadable(backupFile)) {
                throw new MetadataProviderException("Filepath " + backupFile.toAbsolutePath()
                        + " exists but can not be read by this user");
            }
            if (!Files.isWritable(backupFile)) {
                throw new MetadataProviderException("Filepath " + backupFile.toAbsolutePath()
                        + " exists but can not be written to by this user");
            }
        } catch (IOException e) {
            throw new MetadataProviderException("Unable to create backing file " + backupFile.toAbsolutePath(), e);
        }
    }

    private boolean isNonEmpty(Path file) {
        try {
            return Files.size(file) > 0;
        } catch (IOException e) {
            return false;
        }
    }
}
