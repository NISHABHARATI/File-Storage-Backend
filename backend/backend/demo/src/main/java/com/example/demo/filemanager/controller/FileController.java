package com.example.demo.filemanager.controller;

import com.example.demo.filemanager.dto.FileDataResponseDTO;
import com.example.demo.filemanager.entity.FileData;
import com.example.demo.filemanager.service.FileService;
import com.example.demo.filemanager.service.FileSharingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000",allowCredentials = "true")
public class FileController {

    @Autowired
    private FileService fileService;
    @Autowired
    private FileSharingService fileSharingService;


@GetMapping("/list")
public List<FileDataResponseDTO> listFilesAndFolders(@RequestParam Long userId, @RequestParam Long parentId) {
    return fileService.getFilesAndFolders(userId, parentId);
}


    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file,
                                                          @RequestHeader(value = "parentFolderId", required=true) Long parentFolderId,
                                                          @RequestHeader(value = "userId", required = true) Long headerUserId,
                                                          HttpServletRequest request) {
        try {
            Long userId = getUserId(headerUserId, request);
            if (userId == null) {
                return new ResponseEntity<>(Map.of("message", "User not logged in"), HttpStatus.UNAUTHORIZED);
            }

            String fileName = fileService.uploadFile(userId, file, parentFolderId);
            return new ResponseEntity<>(Map.of("fileName", fileName), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", "Failed to upload file"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Object> downloadFile(@RequestHeader("userId") Long userId,
                                               @RequestHeader("fileName") String fileName,
                                               HttpServletRequest request) {
        try {

            if (userId == null) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            FileData file = fileService.getFile(userId, fileName);

            if (file == null || file.getBlobData() == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            String contentType = request.getServletContext().getMimeType(file.getFileName());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            ByteArrayResource resource = new ByteArrayResource(file.getBlobData());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                    .contentLength(file.getBlobData().length)
                    .body(resource);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Long getUserId(Long headerUserId, HttpServletRequest request) {
        if (headerUserId != null) {
            System.out.println("User ID from Header: " + headerUserId);
            return headerUserId;
        }

        HttpSession session = request.getSession();
        Long sessionUserId = (Long) session.getAttribute("userId");
        return sessionUserId;
    }


    @PostMapping("/create-folder")
    public ResponseEntity<FileData> createFolder(@RequestParam Long userId,
                                                 @RequestParam Long parentId,
                                                 @RequestParam String folderName) {
        try {

            FileData folder = FileData.builder()
                    .userId(userId)
                    .parentFolderId(parentId)
                    .fileName(folderName)
                    .isFolder(true)
                    .isFile(false)
                    .createdAt(LocalDateTime.now())
                    .build();
                    FileData savedFolder = fileService.save(folder);

                    return ResponseEntity.ok(savedFolder);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }



    @PutMapping("/rename")
    public ResponseEntity<Object> renameFileOrFolder(@RequestHeader("userId") Long userId,
                                                     @RequestParam("oldFileName") String oldFileName,
                                                     @RequestParam("newFileName") String newFileName) {
        try {
            String renamedFileName = fileService.renameFileOrFolder(userId, oldFileName, newFileName);
            Map<String, String> response = new HashMap<>();
            response.put("message", "File/Folder renamed successfully");
            response.put("newFileName", renamedFileName);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

@GetMapping("/search")
public ResponseEntity<List<FileData>> searchFiles(
        @RequestHeader("userId") Long userId,
        @RequestParam("parentFolderId") Long parentFolderId,
        @RequestParam("searchTerm") String searchTerm)
   {

    List<FileData> matchingFiles = fileService.searchFiles(userId, parentFolderId, searchTerm);
    return new ResponseEntity<>(matchingFiles, HttpStatus.OK);
}
    @PostMapping("/share")
    public ResponseEntity<String> shareFile(
            @RequestParam String fileName,
            @RequestParam String recipientEmail,
            @RequestHeader("userId") Long userId) {
        try {
            if (recipientEmail == null || recipientEmail.isEmpty()) {
                return ResponseEntity.badRequest().body("Recipient email list cannot be empty.");
            }
            fileSharingService.shareFile(fileName, userId, recipientEmail);
            return ResponseEntity.ok("File shared successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/upload-folder")
    public ResponseEntity<Map<String, Object>> uploadFolder(
            @RequestParam("folderName") String folderName,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "parentFolderId", required = false) Long parentFolderId,
            @RequestHeader("userId") Long userId) {

        try {

            fileService.uploadFolder(folderName, files, parentFolderId, userId);

            // Prepare response data
            Map<String, Object> response = new HashMap<>();
            response.put("folderName", folderName);
            response.put("parentFolderId", parentFolderId != null ? parentFolderId : "Root");
            response.put("userId", userId);
            response.put("totalFiles", files.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error uploading folder."));
        }
    }
}


