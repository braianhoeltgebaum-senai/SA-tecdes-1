package com.sa.smart.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.sa.smart.dto.EstoqueDTO;
import com.sa.smart.service.EstoqueService;


public class EstoqueController {

    private final EstoqueService service;

    public EstoqueController(EstoqueService service){
        this.service = service;
    }

    // CREATE
    @PostMapping("/salvar")
    public ResponseEntity<EstoqueDTO> criar(@RequestBody EstoqueDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.criar(dto));
    }

    // READ ALL
    @GetMapping("/listar")
    public ResponseEntity<List<EstoqueDTO>> listar() {
        List<EstoqueDTO> lista = service.listar();
        if (lista.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(lista);
    }

    // READ BY ID
    @GetMapping("/listarId/{id}")
    public ResponseEntity<EstoqueDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    // PUT
    @PutMapping("/put/{id}")
    public ResponseEntity<EstoqueDTO> put(@PathVariable Long id, @RequestBody EstoqueDTO dto) {
        return ResponseEntity.ok(service.put(id, dto));
    }

    // PATCH
    @PatchMapping("/patch/{id}")
    public ResponseEntity<EstoqueDTO> patch(@PathVariable Long id, @RequestBody EstoqueDTO dto) {
        return ResponseEntity.ok(service.patch(id, dto));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}


