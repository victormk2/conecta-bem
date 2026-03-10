package br.com.conectabem.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAnnouncementRequest {

    @NotBlank
    @Size(max = 1000)
    private String message;
}
