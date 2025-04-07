package com.springai.advisor.component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataIngestion {
    private final VectorStore vectorStore;

    @Value("classpath:documents/Effective_java.pdf")
    Resource resource;

    @PostConstruct
    void run() {
        log.info("Loading Files as Documents");
        storeDocument(resource);
    }

    private void storeDocument(Resource resource) {
        log.info("Loading File {} as Document", resource.getFilename());

        DocumentReader pdfReader =
                new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.builder()
                        .build());

        log.info("Splitting document's text");
        List<Document> documents = pdfReader.get();

        documents.forEach(doc -> addResourcePathToDocMetadata(doc, resource));

        List<Document> documentSplits = new TokenTextSplitter().apply(documents);

        log.info("Creating and storing Embeddings from Documents");
        vectorStore.add(documentSplits);
    }

    @SneakyThrows
    private void addResourcePathToDocMetadata(Document doc, Resource resource) {
        doc.getMetadata().put("file_path", resource.getFile().toURI());
    }
}
