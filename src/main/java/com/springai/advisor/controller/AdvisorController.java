package com.springai.advisor.controller;

import com.springai.advisor.service.AdvisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class AdvisorController {
    private final AdvisorService advisorService;

    @PostMapping("/call")
    Flux<String> stream(@RequestBody String query) {
        return advisorService.call(query);
    }
}
