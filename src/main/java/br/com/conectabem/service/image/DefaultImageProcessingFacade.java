package br.com.conectabem.service.image;

import br.com.conectabem.model.Event;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class DefaultImageProcessingFacade implements ImageProcessingFacade {

    @Override
    public void applyIfPresent(Event event, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return;
        }

        if (image.getContentType() == null || !image.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Uploaded file must be an image.");
        }

        try {
            event.setImage(image.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not process uploaded image.");
        }
    }
}

