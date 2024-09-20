package ru.hemulen.docsigner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hemulen.docsigner.model.OssCorpSimRequest;
import ru.hemulen.docsigner.service.ESIAService;

@RestController
@RequestMapping("/esia")
public class ESIAController {
    @Autowired
    private ESIAService esiaService;

    @PostMapping("oss_corp_sim")
    public ResponseEntity sendOssCorpSim(@RequestBody OssCorpSimRequest[] request) {
        esiaService.processOssCorpSimRequest(request);
        return ResponseEntity.ok(request);
    }
}
