package com.rc1.mantra_svc.dto.note;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** Create / update request for a note. */
@Data
public class NoteRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String content;
    private List<String> tags;

    /** Background hex colour, e.g. "#1e1e2e". */
    private String color;
}
