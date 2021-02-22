package io.kidsfirst.web.rest;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/status")
public class HealthResource {

    @GetMapping()
    public ResponseEntity<JSONObject> status() throws IllegalArgumentException {
        val body = new JSONObject();
        body.put("status", "OK");

        return ResponseEntity.ok(body);
    }
}
