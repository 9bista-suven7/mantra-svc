package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.service.ConvertService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * REST controller exposing file-conversion endpoints.
 * All heavy I/O operations are offloaded to the bounded-elastic scheduler.
 */
@RestController
@RequestMapping("/api/convert")
public class ConvertController {

    private final ConvertService svc;

    public ConvertController(ConvertService svc) {
        this.svc = svc;
    }

    /** Converts a JPG or PNG image to PDF. */
    @PostMapping(value = "/image-to-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> imageToPdf(@RequestPart("file") FilePart file) {
        return readFilePart(file)
                .flatMap(bytes -> Mono.fromCallable(() -> svc.imageToPdf(bytes, file.filename()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(pdf -> download(pdf, "converted.pdf", MediaType.APPLICATION_PDF));
    }

    /** Converts the first page of a PDF to a JPEG image. */
    @PostMapping(value = "/pdf-to-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> pdfToImage(@RequestPart("file") FilePart file) {
        return readFilePart(file)
                .flatMap(bytes -> Mono.fromCallable(() -> svc.pdfToImage(bytes))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(img -> download(img, "page1.jpg", MediaType.IMAGE_JPEG));
    }

    /** Splits a PDF into per-page PDFs and returns them as a ZIP. */
    @PostMapping(value = "/pdf-split", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> splitPdf(@RequestPart("file") FilePart file) {
        return readFilePart(file)
                .flatMap(bytes -> Mono.fromCallable(() -> svc.splitPdf(bytes))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(zip -> download(zip, "split_pages.zip", MediaType.parseMediaType("application/zip")));
    }

    /** Merges multiple PDFs into one. */
    @PostMapping(value = "/pdf-merge", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> mergePdfs(@RequestPart("files") List<FilePart> files) {
        return Flux.fromIterable(files)
                .flatMapSequential(this::readFilePart)
                .collectList()
                .flatMap(list -> Mono.<byte[]>fromCallable(() -> svc.mergePdfs(list))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(pdf -> download(pdf, "merged.pdf", MediaType.APPLICATION_PDF));
    }

    /** Converts a DOCX Word file to a plain-text PDF. */
    @PostMapping(value = "/word-to-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> wordToPdf(@RequestPart("file") FilePart file) {
        return readFilePart(file)
                .flatMap(bytes -> Mono.fromCallable(() -> svc.wordToPdf(bytes))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(pdf -> download(pdf, "converted.pdf", MediaType.APPLICATION_PDF));
    }

    /** Extracts text from a PDF and writes it into a DOCX file. */
    @PostMapping(value = "/pdf-to-word", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> pdfToWord(@RequestPart("file") FilePart file) {
        return readFilePart(file)
                .flatMap(bytes -> Mono.fromCallable(() -> svc.pdfToWord(bytes))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(docx -> download(docx, "converted.docx",
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")));
    }

    /** Stamps a diagonal text watermark on every page. */
    @PostMapping(value = "/add-watermark", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> addWatermark(
            @RequestPart("file") FilePart file,
            @RequestPart("text") FormFieldPart text) {
        final String textValue = text.value();
        return readFilePart(file)
                .flatMap(bytes -> Mono.fromCallable(() -> svc.addWatermark(bytes, textValue))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(pdf -> download(pdf, "watermarked.pdf", MediaType.APPLICATION_PDF));
    }

    /** Removes PDF Watermark annotations from all pages. */
    @PostMapping(value = "/remove-watermark", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> removeWatermark(@RequestPart("file") FilePart file) {
        return readFilePart(file)
                .flatMap(bytes -> Mono.fromCallable(() -> svc.removeWatermark(bytes))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(pdf -> download(pdf, "cleaned.pdf", MediaType.APPLICATION_PDF));
    }

    /** Password-protects a PDF. */
    @PostMapping(value = "/lock-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> lockPdf(
            @RequestPart("file") FilePart file,
            @RequestPart("password") FormFieldPart password) {
        final String pwd = password.value();
        return readFilePart(file)
                .flatMap(bytes -> Mono.fromCallable(() -> svc.lockPdf(bytes, pwd))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(pdf -> download(pdf, "locked.pdf", MediaType.APPLICATION_PDF));
    }

    /** Removes password protection from a PDF. */
    @PostMapping(value = "/unlock-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> unlockPdf(
            @RequestPart("file") FilePart file,
            @RequestPart("password") FormFieldPart password) {
        final String pwd = password.value();
        return readFilePart(file)
                .flatMap(bytes -> Mono.fromCallable(() -> svc.unlockPdf(bytes, pwd))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(pdf -> download(pdf, "unlocked.pdf", MediaType.APPLICATION_PDF));
    }

    /** Extracts specific pages (e.g., "1,3,5-8") into a new PDF. */
    @PostMapping(value = "/extract-pages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> extractPages(
            @RequestPart("file") FilePart file,
            @RequestPart("pages") FormFieldPart pages) {
        final String pageSpec = pages.value();
        return readFilePart(file)
                .flatMap(bytes -> Mono.fromCallable(() -> svc.extractPages(bytes, pageSpec))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(pdf -> download(pdf, "extracted.pdf", MediaType.APPLICATION_PDF));
    }

    /** Overlays text on a specific page of the PDF. */
    @PostMapping(value = "/add-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> addText(
            @RequestPart("file") FilePart file,
            @RequestPart("text") FormFieldPart text,
            @RequestPart("page") FormFieldPart page) {
        final String textValue = text.value();
        int pageNum = 1;
        try { pageNum = Integer.parseInt(page.value().trim()); } catch (NumberFormatException ignored) {}
        final int p = pageNum;
        return readFilePart(file)
                .flatMap(bytes -> Mono.fromCallable(() -> svc.addTextToPdf(bytes, textValue, p))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(pdf -> download(pdf, "annotated.pdf", MediaType.APPLICATION_PDF));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Collects all DataBuffer chunks from a FilePart into a single byte array. */
    private Mono<byte[]> readFilePart(FilePart filePart) {
        return filePart.content()
                .reduce(new byte[0], (acc, buf) -> {
                    byte[] chunk = new byte[buf.readableByteCount()];
                    buf.read(chunk);
                    byte[] combined = new byte[acc.length + chunk.length];
                    System.arraycopy(acc, 0, combined, 0, acc.length);
                    System.arraycopy(chunk, 0, combined, acc.length, chunk.length);
                    return combined;
                });
    }

    /** Builds a file-download ResponseEntity. */
    private ResponseEntity<byte[]> download(byte[] body, String filename, MediaType contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(contentType)
                .body(body);
    }
}
