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
import com.sa.smart.dto.ExpedicaoDTO;
import com.sa.smart.service.ExpedicaoService;


public class ExpedicaoController {

    private final ExpedicaoService service;

    public ExpedicaoController(ExpedicaoService service){

        this.service = service;

    }

    @PostMapping("/salvar")
    public ResponseEntity<ExpedicaoDTO> criar(@RequestBody ExpedicaoDTO dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.criar(dto));

    }

    @GetMapping("/listar")
    public ResponseEntity<List<ExpedicaoDTO>> listar() {

        List<ExpedicaoDTO> lista = service.listar();
        if (lista.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(lista);

    }

    @GetMapping("/listarId/{id}")
    public ResponseEntity<ExpedicaoDTO> buscar(@PathVariable Long id) {

        return ResponseEntity.ok(service.buscar(id));
        
    }

    @PutMapping("/put/{id}")
    public ResponseEntity<ExpedicaoDTO> atualizarTotal(@PathVariable Long id, @RequestBody ExpedicaoDTO dto) {

        return ResponseEntity.ok(service.put(id, dto));

    }

    @PatchMapping("/patch/{id}")
    public ResponseEntity<ExpedicaoDTO> atualizarParcial(@PathVariable Long id, @RequestBody ExpedicaoDTO dto) {
        
        return ResponseEntity.ok(service.patch(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {

        service.deletar(id);
        return ResponseEntity.noContent().build();
        
    }
}