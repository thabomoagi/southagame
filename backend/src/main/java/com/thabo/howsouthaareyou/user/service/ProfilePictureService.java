package com.thabo.howsouthaareyou.user.service;

import com.thabo.howsouthaareyou.common.exception.BadRequestException;
import com.thabo.howsouthaareyou.config.UploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfilePictureService {

    private static final long MAX_INPUT_SIZE = 10 * 1024 * 1024;
    private static final long MAX_OUTPUT_SIZE = 90 * 1024;
    private static final int MAX_DIMENSION = 512;

    private final UploadProperties uploadProperties;

    public String store(MultipartFile file, UUID userId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No image file provided");
        }

        if (file.getSize() > MAX_INPUT_SIZE) {
            throw new BadRequestException("Image must be smaller than 10 MB");
        }

        try {
            BufferedImage image = readValidatedImage(file);
            byte[] webp = encodeWebp(image);

            String filename = "user_" + userId + "_" + System.currentTimeMillis() + ".webp";
            Path uploadDirectory = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize();
            Path target = uploadDirectory.resolve(filename).normalize();

            if (!target.startsWith(uploadDirectory)) {
                throw new BadRequestException("Invalid image path");
            }

            Files.createDirectories(uploadDirectory);
            Files.write(target, webp);

            log.info("Profile picture saved: {} ({} bytes)", filename, webp.length);
            return uploadProperties.getUrlPrefix() + "/" + filename;
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            log.error("Failed to process profile picture", exception);
            throw new BadRequestException("Unable to process profile picture");
        }
    }

    private BufferedImage readValidatedImage(MultipartFile file) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) {
                throw new BadRequestException("File is not a valid image");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BadRequestException("Unsupported image type or not a valid image");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0) {
                    throw new BadRequestException("File is not a valid image");
                }

                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new BadRequestException("File is not a valid image");
                }

                if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    image = resizeImage(image, width, height);
                }

                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int originalWidth, int originalHeight) {
        int targetWidth = originalWidth;
        int targetHeight = originalHeight;

        if (originalWidth > originalHeight) {
            targetWidth = MAX_DIMENSION;
            targetHeight = (int) Math.round((double) originalHeight * MAX_DIMENSION / originalWidth);
        } else {
            targetHeight = MAX_DIMENSION;
            targetWidth = (int) Math.round((double) originalWidth * MAX_DIMENSION / originalHeight);
        }

        targetWidth = Math.max(1, targetWidth);
        targetHeight = Math.max(1, targetHeight);

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        return resizedImage;
    }

    private byte[] encodeWebp(BufferedImage image) throws IOException {
        for (float quality = 0.85f; quality >= 0.4f; quality -= 0.05f) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
            if (!writers.hasNext()) {
                throw new BadRequestException("WebP image processing is unavailable");
            }

            ImageWriter writer = writers.next();
            try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
                writer.setOutput(output);
                ImageWriteParam parameters = writer.getDefaultWriteParam();
                if (parameters.canWriteCompressed()) {
                    parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    String[] compressionTypes = parameters.getCompressionTypes();
                    if (compressionTypes != null && compressionTypes.length > 0) {
                        parameters.setCompressionType(compressionTypes[0]);
                    }
                    parameters.setCompressionQuality(quality);
                }
                writer.write(null, new IIOImage(image, null, null), parameters);
            } finally {
                writer.dispose();
            }

            if (bytes.size() <= MAX_OUTPUT_SIZE) {
                return bytes.toByteArray();
            }
        }

        throw new BadRequestException("Unable to compress image below 90 KB");
    }
}
