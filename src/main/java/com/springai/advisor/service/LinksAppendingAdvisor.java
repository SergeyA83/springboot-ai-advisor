package com.springai.advisor.service;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;

import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class LinksAppendingAdvisor implements BaseAdvisor {
    private int order = 0;

    @Nonnull
    public AdvisedRequest before(@Nonnull AdvisedRequest request) {
        return request;
    }

    @Nonnull
    public AdvisedResponse after(AdvisedResponse advisedResponse) {
        ChatResponse.Builder chatResponseBuilder;

        if (advisedResponse.response() == null) {
            chatResponseBuilder = ChatResponse.builder();
        } else {
            chatResponseBuilder = ChatResponse.builder().from(advisedResponse.response());

            List<Generation> generations = new ArrayList<>(advisedResponse.response().getResults());
            Generation generation = generations.getLast();

            String linksFooter = getLinksFooter(advisedResponse);

            log.info("Adding links footer: {}", linksFooter);
            generations.set(generations.size() - 1,
                    new Generation(new AssistantMessage(generation.getOutput().getText() + linksFooter),
                            generation.getMetadata()));

            chatResponseBuilder.generations(generations);
        }

        return new AdvisedResponse(chatResponseBuilder.build(), advisedResponse.adviseContext());
    }

    private String getLinksFooter(AdvisedResponse advisedResponse) {
        List<?> documents = (ArrayList<?>)
                advisedResponse
                        .adviseContext()
                        .get("rag_document_context");

        if (documents == null || documents.isEmpty()) {
            return "";
        }

        Map<String, Set<Integer>> filePathToPagesMap =
                documents
                        .stream()
                        .map(doc -> (Document) doc)
                        .collect(Collectors.groupingBy(
                                doc -> (String) doc.getMetadata().get("file_path"),
                                Collectors.mapping(
                                        doc -> (Integer) doc.getMetadata().get("page_number"),
                                        Collectors.toCollection(TreeSet::new))));

        StringBuilder footer = new StringBuilder();
        footer.append("\n\n---------------------------------------\n Links:");

        filePathToPagesMap
                .forEach((key, value) -> footer.append("\n ")
                        .append(key)
                        .append(", pages: ")
                        .append(String.join(",", String.valueOf(value))));

        return footer.toString();
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
