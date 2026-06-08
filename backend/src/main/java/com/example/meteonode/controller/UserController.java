package com.example.meteonode.controller;

import com.example.meteonode.model.dto.request.UpdateRoleRequest;
import com.example.meteonode.model.dto.response.UserDTO;
import com.example.meteonode.service.domain.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "User profile and administration. Any authenticated user can read their own profile. Listing all users, changing roles, and deleting accounts require ADMIN.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "List all users")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User list"),
        @ApiResponse(responseCode = "403", description = "Not ADMIN", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Change a user's role", description = "Valid roles: USER, OPERATOR, ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Role updated", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid role value", content = @Content),
        @ApiResponse(responseCode = "403", description = "Not ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/role")
    public ResponseEntity<Void> setUserRole(@RequestBody UpdateRoleRequest request) {
        userService.setUserRole(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted", content = @Content),
        @ApiResponse(responseCode = "403", description = "Not ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }
}
