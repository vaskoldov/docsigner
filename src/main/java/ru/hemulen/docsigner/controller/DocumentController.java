package ru.hemulen.docsigner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hemulen.docsigner.exception.*;
import ru.hemulen.docsigner.model.*;
import ru.hemulen.docsigner.model.Error;
import ru.hemulen.docsigner.service.DocumentService;

@RestController
@RequestMapping("/document/sign")
public class DocumentController {
    @Autowired
    private DocumentService documentService;

    @PostMapping(value = "unep")
    public ResponseEntity sendDocument(@RequestBody Document document) {
        try {
            documentService.processDocument(document);
            DocumentResponse response = new DocumentResponse(new Content(document.getClientId()));
            return ResponseEntity.ok(response);
        } catch (DocumentFileNotExists e) {
            DocumentResponse response = new DocumentResponse(new Error("DOCUMENT_NOT_EXISTS",
                    String.format("По указанному пути - %s - файл документа не найден", document.getDocumentPath())));
            return ResponseEntity.badRequest().body(response);
        } catch (DocumentSignException e) {
            DocumentResponse response = new DocumentResponse(new Error("SIGN_ERROR",
                    String.format("Не удалось подписать файл %s", document.getDocumentPath())));
            return ResponseEntity.badRequest().body(response);
        } catch (FileOperationsException e) {
            DocumentResponse response = new DocumentResponse(new Error("FILE_MOVE_ERROR",
                    String.format("Не удалось переместить файл %s", document.getDocumentPath())));
            return ResponseEntity.badRequest().body(response);
        } catch (XMLTransformationException e) {
            DocumentResponse response = new DocumentResponse(new Error("XML_ERROR",
                    String.format("Не удалось сформировать ClientMessage для файла %s", document.getDocumentPath())));
            return ResponseEntity.badRequest().body(response);
        } catch (IncorrectParameterException e) {
            DocumentResponse response = new DocumentResponse(new Error("INCORRECT_DATE_FORMAT",
                    String.format("Некорректный формат даты signExp %s. Допустимый формат \"yyyy-MM-dd'T'HH:mm:ss\".", document.getSignExp())));
            return ResponseEntity.badRequest().body(response);
        }

    }

    @PostMapping("ukep")
    public ResponseEntity sendDocumentUKEP(@RequestBody Document document) {
        try {
            documentService.processDocumentUKEP(document);
            DocumentResponse response = new DocumentResponse(new Content(document.getClientId()));
            return ResponseEntity.ok(response);
        } catch (DocumentFileNotExists e) {
            DocumentResponse response = new DocumentResponse(new Error("DOCUMENT_NOT_EXISTS",
                    String.format("По указанному пути - %s - файл документа не найден", document.getDocumentPath())));
            return ResponseEntity.badRequest().body(response);
        } catch (DocumentSignException e) {
            DocumentResponse response = new DocumentResponse(new Error("SIGN_ERROR",
                    String.format("Не удалось подписать файл %s", document.getDocumentPath())));
            return ResponseEntity.badRequest().body(response);
        } catch (FileOperationsException e) {
            DocumentResponse response = new DocumentResponse(new Error("FILE_MOVE_ERROR",
                    String.format("Не удалось переместить файл %s", document.getDocumentPath())));
            return ResponseEntity.badRequest().body(response);
        } catch (XMLTransformationException e) {
            DocumentResponse response = new DocumentResponse(new Error("XML_ERROR",
                    String.format("Не удалось сформировать ClientMessage для файла %s", document.getDocumentPath())));
            return ResponseEntity.badRequest().body(response);
        } catch (IncorrectParameterException e) {
            DocumentResponse response = new DocumentResponse(new Error("INCORRECT_DATE_FORMAT",
                    String.format("Некорректный формат даты signExp %s. Допустимый формат \"yyyy-MM-dd'T'HH:mm:ss\".", document.getSignExp())));
            return ResponseEntity.badRequest().body(response);
        }
    }



}
