package com.socialnet.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.socialnet.dto.response.CommonRs;
import com.socialnet.entity.other.Storage;
import com.socialnet.exception.BadRequestException;
import com.socialnet.service.StorageService;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "StorageController", description = "Upload users profile image")
public class StorageController {

    private final StorageService storageService;

    @ApiResponse(responseCode = "200")
    @PostMapping(consumes = "multipart/form-data", produces = "application/json")
    public CommonRs<Storage> uploadProfileImage(
            @RequestParam("type") @Parameter(example = "IMAGE") String type,
            @RequestBody MultipartFile file)
            throws BadRequestException, IOException {

        return storageService.uploadProfileImage(type, file);
    }
}
