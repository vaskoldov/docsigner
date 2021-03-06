package ru.hemulen.docsigner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hemulen.docsigner.model.Content;
import ru.hemulen.docsigner.model.Document;
import ru.hemulen.docsigner.model.DocumentResponse;
import ru.hemulen.docsigner.model.Error;
import ru.hemulen.docsigner.exception.DocumentFileNotExists;
import ru.hemulen.docsigner.exception.DocumentSignException;
import ru.hemulen.docsigner.exception.FileOperationsException;
import ru.hemulen.docsigner.exception.XMLTransformationException;
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
        }
    }


}
