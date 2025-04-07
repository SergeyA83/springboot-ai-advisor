package com.springai.advisor.component;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
public class LinksFooterAdvisor implements BaseAdvisor {
    private int order = 0;

    @Nonnull
    public AdvisedRequest before(@Nonnull AdvisedRequest request) {
        return request;
    }

    @Nonnull
    public AdvisedResponse after(AdvisedResponse advisedResponse) {
        List<?> documents = (ArrayList<?>) advisedResponse
                .adviseContext()
                .get("rag_document_context");

        if (documents.isEmpty()) {
            return advisedResponse;
        }

        String linksFooter = getLinksFooter(documents);

        ChatResponse.Builder chatResponseBuilder;

        if (advisedResponse.response() == null) {
            chatResponseBuilder = ChatResponse.builder();
        } else {
            chatResponseBuilder = ChatResponse.builder().from(advisedResponse.response());

            List<Generation> generations;

            generations = new ArrayList<>(advisedResponse.response().getResults());
            Generation generation = generations.getLast();
            generations.set(generations.size() - 1,
                    new Generation(new AssistantMessage(generation.getOutput().getText() + linksFooter),
                            generation.getMetadata()));

            chatResponseBuilder.generations(generations);
        }

        return new AdvisedResponse(chatResponseBuilder.build(), advisedResponse.adviseContext());
    }

    private String getLinksFooter(List<?> documents) {
        Map<String, Set<Integer>> filePathToPagesMap = documents
                .stream()
                .map(doc -> (Document) doc)
                .collect(Collectors.groupingBy(
                        doc -> (String) doc.getMetadata().get("file_path"),
                        Collectors.mapping(
                                doc -> (Integer) doc.getMetadata().get("page_number"),
                                Collectors.toCollection(TreeSet::new))));

        return "\n \n---------------------------------------\n Links:" +
                filePathToPagesMap.entrySet()
                        .stream()
                        .map(entry -> String.format("\n %s, pages: %s",
                                entry.getKey(),
                                String.join(",", String.valueOf(entry.getValue()))))
                        .collect(Collectors.joining("\n"));
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
