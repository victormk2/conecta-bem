package br.com.conectabem.service.image;

import br.com.conectabem.model.Event;
import org.springframework.web.multipart.MultipartFile;

public interface ImageProcessingFacade {
    void applyIfPresent(Event event, MultipartFile image);
}

