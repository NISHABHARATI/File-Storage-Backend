package com.example.demo.filemanager.service;
import com.example.demo.filemanager.dto.FileDataResponseDTO;
import com.example.demo.filemanager.entity.FileData;
import com.example.demo.filemanager.repository.FileRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileService {

    @Autowired
    FileRepository fileRepository;


    public List<FileDataResponseDTO> getFilesAndFolders(Long userId, Long parentFolderId) {
        List<FileData> fileDataList = fileRepository.findByUserIdAndParentFolderId(userId, parentFolderId);
        return fileDataList.stream().map(fileData -> FileDataResponseDTO.builder()
                .userId(fileData.getUserId())
                .parentFolderId(fileData.getParentFolderId())
                .tableId(fileData.getTableId())
                .fileName(fileData.getFileName())
                .isFolder(fileData.getIsFolder())
                .isFile(fileData.getIsFile())
                .path(fileData.getPath())
                .build()).collect(Collectors.toList());
    }

    public FileData getFile(Long userId, String fileName) {
        return fileRepository.findByUserIdAndFileName(userId, fileName);
    }


    public String uploadFile(Long userId, MultipartFile file, Long parentFolderId) throws IOException {
        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty.");
        }

        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));

        List<FileData> existingFiles = fileRepository.findByUserIdAndParentFolderId(userId, parentFolderId);

        Set<String> existingFileNames = existingFiles.stream()
                .map(FileData::getFileName)
                .collect(Collectors.toSet());
        int version = 1;

        String newFileName = originalFileName;
        while (existingFileNames.contains(newFileName)) {
            newFileName = baseName + "_v" + version + extension;
            version++;
        }

        FileData fileData = new FileData();
        fileData.setUserId(userId);
        fileData.setParentFolderId(parentFolderId);
        fileData.setFileName(newFileName);
        fileData.setIsFolder(false);
        fileData.setIsFile(true);
        fileData.setBlobData(file.getBytes());
        fileData.setCreatedAt(LocalDateTime.now());

        if (parentFolderId == -1) {
            fileData.setPath(null);
        } else {
            FileData parentFolder = fileRepository.findById(parentFolderId).orElse(null);
            if (parentFolder != null) {
                fileData.setPath(parentFolder.getPath() + "/" + newFileName);
            } else {
                fileData.setPath("/" + newFileName);
            }
        }

        fileRepository.save(fileData);
        fileRepository.flush();

        return fileData.getFileName();
    }

    public FileData save(FileData fileData) {
        fileRepository.save(fileData);
        return fileData;
    }

    public String renameFileOrFolder(Long userId, String oldFileName, String newFileName) {

        FileData fileData = fileRepository.findByUserIdAndFileName(userId, oldFileName);
        if (fileData == null) {
            return "File or folder not found";
        }
        if (oldFileName.equals(newFileName)) return "File Name already exists";
        fileData.setFileName(newFileName);
        fileRepository.save(fileData);
        return newFileName;
    }


    public List<FileData> searchFiles(Long userId, Long parentFolderId, String searchTerm) {
        List<FileData> results = new ArrayList<>(); // Correctly initialized as List<FileData>
        searchInFolder(userId, parentFolderId, searchTerm, results); // Start recursive search
        return results; // Return found results
    }


    private void searchInFolder(Long userId, Long parentFolderId, String searchTerm, List<FileData> results) {
        // Fetch files in the current folder
        List<FileData> files = fileRepository.findByUserIdAndParentFolderId(userId, parentFolderId);
        for (FileData file : files) {
            if (file.getFileName().toLowerCase().startsWith(searchTerm.toLowerCase())) {
                results.add(file);
            }
        }

        List<FileData> folders = files.stream().filter(FileData::getIsFolder).collect(Collectors.toList());
        for (FileData folder : folders) {
            searchInFolder(userId, folder.getTableId(), searchTerm, results);
        }
    }
    public void uploadFolder(String folderName, List<MultipartFile> files, Long parentFolderId, Long userId) throws IOException {

        FileData folder = new FileData();
        folder.setFileName(folderName);
        folder.setIsFolder(true);
        folder.setIsFile(false);
        folder.setBlobData(null);
        folder.setParentFolderId(parentFolderId);
        folder.setUserId(userId);
        folder.setCreatedAt(LocalDateTime.now());

        fileRepository.save(folder);

        for (MultipartFile file : files) {
            String originalFileName = file.getOriginalFilename();

            if (!file.isEmpty()) {
                FileData fileData = new FileData();
                fileData.setFileName(originalFileName);
                fileData.setIsFolder(false);
                fileData.setIsFile(true);
                fileData.setBlobData(file.getBytes());
                fileData.setParentFolderId(folder.getTableId());
                fileData.setUserId(userId);
                fileData.setCreatedAt(LocalDateTime.now());

                fileRepository.save(fileData);
            }
        }
    }
}