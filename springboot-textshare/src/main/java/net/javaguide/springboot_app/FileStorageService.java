package net.javaguide.springboot_app;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileStorageService {
    private final Path storageFolder = Paths.get("uploads");
    private final Map<String, String> fileCodeMap = new ConcurrentHashMap<>();

    public FileStorageService() throws IOException {
        if (!Files.exists(storageFolder)) {
            Files.createDirectories(storageFolder);
        }
    }

    /**
     * Stores the file and returns a unique code for retrieval
     */
    public String store(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileCode = generateUniqueCode();
        
        // Store using the original filename prefixed with the code to avoid collisions
        String storedFilename = fileCode + "_" + originalFilename;
        Path destination = storageFolder.resolve(storedFilename);
        
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        
        // Map the code to the stored filename
        fileCodeMap.put(fileCode, storedFilename);
        
        return fileCode;
    }

    /**
     * Retrieves a file path by code
     */
    public Path getFilePathByCode(String fileCode) {
        String storedFilename = fileCodeMap.get(fileCode);
        if (storedFilename == null) {
            throw new RuntimeException("File not found for code: " + fileCode);
        }
        
        return storageFolder.resolve(storedFilename);
    }
    
    /**
     * Gets the original filename from the stored filename
     */
    public String getOriginalFilename(String storedFilename) {
        // Remove the code prefix (everything before the first underscore)
        int underscoreIndex = storedFilename.indexOf('_');
        return storedFilename.substring(underscoreIndex + 1);
    }

    /**
     * Lists all stored files
     */
    public Map<String, String> listFiles() {
        try (Stream<Path> paths = Files.list(storageFolder)) {
            return paths
                .filter(Files::isRegularFile)
                .collect(Collectors.toMap(
                    path -> path.getFileName().toString().split("_", 2)[0],  // The code
                    path -> path.getFileName().toString()  // The stored filename
                ));
        } catch (IOException e) {
            throw new RuntimeException("Failed to list stored files", e);
        }
    }
    
    /**
     * Generates a unique 6-character code
     */
    private String generateUniqueCode() {
        return UUID.randomUUID().toString().substring(0, 6);
    }
}