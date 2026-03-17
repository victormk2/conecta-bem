package br.com.conectabem.dto.event;

import br.com.conectabem.model.ParticipationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateParticipationStatusRequest {

    @NotNull
    private ParticipationStatus status;
}
