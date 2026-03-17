package br.com.conectabem.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JustifyAbsenceRequest {

    @NotBlank
    @Size(max = 2000)
    private String justification;
}
