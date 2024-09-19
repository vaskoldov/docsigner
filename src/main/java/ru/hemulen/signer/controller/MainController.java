package ru.hemulen.signer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hemulen.signer.exception.DocumentFileNotExists;
import ru.hemulen.signer.exception.DocumentSignException;
import ru.hemulen.signer.exception.InvalidRequestParameters;
import ru.hemulen.signer.exception.InvalidXMLException;
import ru.hemulen.signer.model.Error;
import ru.hemulen.signer.model.Request;
import ru.hemulen.signer.model.Response;
import ru.hemulen.signer.service.SignService;

@RestController
@RequestMapping("/api/v1")
public class MainController {
    @Autowired
    private SignService signService;

    @PostMapping(value = "pkcs7")
    public ResponseEntity signPKCS7(@RequestBody Request request) {
        Response response = new Response();
        try {
            String signPath = signService.processPKCS7(request);
            response.setSignPath(signPath);
            return ResponseEntity.ok(response);
        } catch (DocumentFileNotExists e) {
            Error error = new Error("DOCUMENT_NOT_EXISTS",
                    String.format("По указанному пути - %s - файл документа не найден", request.getFilePath()));
            response.setError(error);
            return ResponseEntity.badRequest().body(response);
        } catch (DocumentSignException e) {
            Error error = new Error("SIGN_ERROR",
                    String.format("Не удалось подписать файл %s", request.getFilePath()));
            response.setError(error);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("xmldsig")
    public ResponseEntity sendXMLDSig(@RequestBody Request request) {
        Response response = new Response();
        try {
            String signFilePath = signService.processXMLDSig(request);
            response.setSignPath(signFilePath);
            return ResponseEntity.ok(response);
        } catch (DocumentFileNotExists e) {
            Error error = new Error("DOCUMENT_NOT_EXISTS",
                    String.format("По указанному пути - %s - файл документа не найден", request.getFilePath()));
            response.setError(error);
            return ResponseEntity.badRequest().body(response);
        } catch (DocumentSignException e) {
            Error error = new Error("SIGN_ERROR",
                    String.format("Не удалось подписать файл %s", request.getFilePath()));
            response.setError(error);
            return ResponseEntity.badRequest().body(response);
        } catch (InvalidRequestParameters e) {
            Error error = new Error("INVALID_REQUEST_PARAMETERS",
                    "Не указан подписываемый XML-элемент");
            response.setError(error);
            return ResponseEntity.badRequest().body(response);
        } catch (InvalidXMLException e) {
            Error error = new Error ("INVALID_XML",
                    "В файле либо отсутствует подписываемый элемент, либо содержится более одного подписываемого элемента");
            response.setError(error);
            return ResponseEntity.badRequest().body(response);
        }
    }


}
