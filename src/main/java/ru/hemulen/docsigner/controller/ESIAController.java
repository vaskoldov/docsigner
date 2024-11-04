package ru.hemulen.docsigner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.hemulen.docsigner.model.OssCorpSimRequest;
import ru.hemulen.docsigner.model.OssCorpSimResponse;
import ru.hemulen.docsigner.service.ESIAService;

import javax.xml.parsers.ParserConfigurationException;

@RestController
@RequestMapping("/esia")
public class ESIAController {
    @Autowired
    private ESIAService esiaService;

    @PostMapping("oss_corp_sim")
    public ResponseEntity sendOssCorpSim(@RequestBody OssCorpSimRequest[] request) {
        try {
            return esiaService.processOssCorpSimRequest(request);
        } catch (ParserConfigurationException e) {
            return (ResponseEntity) ResponseEntity.internalServerError();
        }
    }

    @GetMapping("oss_corp_sim/{clientId}")
    public ResponseEntity getOssSimResponse(@RequestParam String clientId) {
        try {
            return esiaService.processOssCorpSimResponse(clientId);
        } catch (Exception e) {
            return (ResponseEntity) ResponseEntity.internalServerError();
        }
    }
}
