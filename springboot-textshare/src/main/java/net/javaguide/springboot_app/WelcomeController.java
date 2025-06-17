package net.javaguide.springboot_app;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
public class WelcomeController {
    
    private final FileStorageService fileStorageService;
    private final Map<String, String> textStorage = new ConcurrentHashMap<>();

    // Constructor injection
    public WelcomeController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }
    
    @GetMapping("/welcome")
    public String index() {
        return "index";
    }

    @PostMapping("/save")
    public String saveText(@RequestParam("content") String content, Model model) {
        if (content == null || content.trim().isEmpty()) {
            model.addAttribute("error", "Content cannot be empty!");
            return "index";
        }

        String id = UUID.randomUUID().toString().substring(0, 6);
        textStorage.put(id, content);

        model.addAttribute("success", "Message saved successfully! Your code is: " + id);
        return "index";
    }

    @GetMapping("/view")
    public String viewText(@RequestParam String id, Model model) {
        String content = textStorage.get(id);

        if (content == null) {
            model.addAttribute("error", "No message found for ID: " + id);
            return "index";
        }

        model.addAttribute("content", content);
        return "view";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/";
            }
            
            String fileCode = fileStorageService.store(file);
            redirectAttributes.addFlashAttribute("success", 
                "File uploaded successfully! Your file code is: " + fileCode);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to upload file: " + e.getMessage());
        }
        return "redirect:/";
    }
    
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("fileId") String fileCode) {
        try {
            Path filePath = fileStorageService.getFilePathByCode(fileCode);
            String filename = fileStorageService.getOriginalFilename(filePath.getFileName().toString());
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
            } else {
                throw new RuntimeException("File not found: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("File download failed", e);
        }
    }
    
    // API to list files (for JavaScript to load)
    @GetMapping("/api/files")
    @ResponseBody
    public Map<String, String> listFiles() {
        Map<String, String> fileCodeMap = fileStorageService.listFiles();
        
        // Convert to a map of code -> original filename
        return fileCodeMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> fileStorageService.getOriginalFilename(entry.getValue())
            ));
    }
}