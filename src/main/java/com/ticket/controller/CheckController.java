package com.ticket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping
public class CheckController {

    @GetMapping("/check")
    public ResponseEntity<?> check() {
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("status", "UP"));
    }

}
