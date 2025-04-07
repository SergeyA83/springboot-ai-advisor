package com.springai.advisor.service;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
@Slf4j
public class QueryLinksAugmenter implements QueryAugmenter {
    private static final PromptTemplate PROMPT_TEMPLATE
            = new PromptTemplate("""
            Context information is below.
            ---------------------
            {context}
            ---------------------
            Given the context information and no prior knowledge, answer the query.
            Follow these rules:
            1. If the answer is not in the context, just say "Follow links provided below.".
            2. Avoid statements like "Based on the context..." or "The provided information...".
            Query: {query}
            Answer:""");

    private static final PromptTemplate EMPTY_CONTEXT_PROMPT_TEMPLATE
            = new PromptTemplate("""
            The user query is outside your knowledge base.
            Politely inform the user that you can't answer it.""");

    @NonNull
    public Query augment(@NonNull Query query, @NonNull List<Document> documents) {
        log.debug("Augmenting query with contextual data, query: {}", query.text());
        if (documents.isEmpty()) {
            return augmentQueryWhenEmptyContext(query);
        } else {
            return augmentQuery(query, documents);
        }
    }

    private Query augmentQuery(Query query, List<Document> documents) {
        String documentContext = documents.
                stream()
                .map(Document::getText).
                collect(Collectors.joining(System.lineSeparator()));

        log.info("Augment context: {}", documentContext);
        Map<String, Object> promptParameters = Map.of("query", query.text(),
                "context", documentContext);
        return new Query(PROMPT_TEMPLATE.render(promptParameters));
    }

    private Query augmentQueryWhenEmptyContext(Query query) {
        return new Query(EMPTY_CONTEXT_PROMPT_TEMPLATE.render());
    }
}
