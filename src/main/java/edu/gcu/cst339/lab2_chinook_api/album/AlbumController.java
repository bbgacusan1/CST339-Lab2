package edu.gcu.cst339.lab2_chinook_api.album;

import java.net.URI;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

/**
 * REST API for CRUD operations on Chinook Albums
 * AlbumController
 */
@RestController
@RequestMapping("/api/albums")
class AlbumController {
    
    private final AlbumService albumService;

    AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    /**
     * Returns all albums.
     *
     * @return list of all albums
     */
    @GetMapping
    List<AlbumDto> getAll() {
        return albumService.findAll();
    }

    /**
     * Returns a single album by its ID.
     *
     * @param id the ID of the album
     * @return the album with the specified ID
     * @throws AlbumNotFoundException if the album with the specified ID does not exist
     */
    @GetMapping("/{id}")
    AlbumDto getById(@PathVariable Integer id) {
        return albumService.findById(id);
    }
    
    /**
     * Creates a new album.
     *
     * @param dto the album data to create
     * @return the created album
     */
    @PostMapping
    ResponseEntity<AlbumDto> create(@RequestBody @Valid AlbumDto dto) {
        AlbumDto createdAlbum = albumService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(createdAlbum.albumId())
            .toUri();
        return ResponseEntity.created(location).body(createdAlbum);
    }

    /**
     * Updates a single album by its ID.
     *
     * @param id the ID of the album to update
     * @param dto the album data to update
     * @return the updated album
     * @throws AlbumNotFoundException if the album with the specified ID does not exist
     */
    @PutMapping("/{id}")
    ResponseEntity<AlbumDto> update(@PathVariable Integer id, @RequestBody @Valid AlbumDto dto) {
        AlbumDto updatedAlbum = albumService.update(id, dto);
        return ResponseEntity.ok(updatedAlbum);
    }

    /**
     * Deletes a single album by its ID.
     *
     * @param id the ID of the album to delete
     * @throws AlbumNotFoundException if the album with the specified ID does not exist
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Integer id) {
        albumService.delete(id);
    }

    /**
     * Converts database constraint violations into HTTP 409 Conflict responses.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<String> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("Request conflicts with existing data (foreign key constraint).");
    }
}
