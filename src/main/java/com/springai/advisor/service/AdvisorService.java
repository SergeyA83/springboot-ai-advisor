package com.springai.advisor.service;

import com.springai.advisor.component.ContextAndLinksQueryAugmenter;
import com.springai.advisor.component.LinksFooterAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AdvisorService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AdvisorService(ChatClient.Builder chatClientBuilder,
                          VectorStore vectorStore) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.vectorStore = vectorStore;
    }

    public Flux<String> call(String userMessageContent) {
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.7)
                .build();

        QueryAugmenter queryAugmenter = new ContextAndLinksQueryAugmenter();

        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();

        LinksFooterAdvisor linksFooterAdvisor = new LinksFooterAdvisor();

        return this.chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor,
                        linksFooterAdvisor)
                .user(userMessageContent)
                .stream()
                .content();
    }
}
