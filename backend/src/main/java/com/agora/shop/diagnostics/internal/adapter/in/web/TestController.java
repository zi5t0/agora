package com.agora.shop.diagnostics.internal.adapter.in.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestController {

    @GetMapping(path = "/api/v1/test", produces = MediaType.APPLICATION_JSON_VALUE)
    TestResponse test() {
        return new TestResponse("Esto es una prueba");
    }

    record TestResponse(String message) {
    }
}
