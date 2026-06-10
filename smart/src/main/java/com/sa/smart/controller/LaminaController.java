package com.sa.smart.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sa.smart.dto.LaminaDTO;
import com.sa.smart.service.LaminaService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/lamina")
public class LaminaController {

    private final LaminaService service;

    public LaminaController(LaminaService service){

        this.service = service;

    }

    @PostMapping("/salvar")
    public ResponseEntity<LaminaDTO> criar(@RequestBody LaminaDTO dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.criar(dto));

    }

    @GetMapping("/listar")
    public ResponseEntity<List<LaminaDTO>> listar() {

        List<LaminaDTO> lista = service.listar();
        if (lista.isEmpty()) return ResponseEntity.noContent().build();

        return ResponseEntity.ok(lista);

    }

    @GetMapping("/listarId/{id}")
    public ResponseEntity<LaminaDTO> buscar(@PathVariable Long id) {

        return ResponseEntity.ok(service.buscar(id));

    }

    @PutMapping("/put/{id}")
    public ResponseEntity<LaminaDTO> atualizarTotal(@PathVariable Long id, @RequestBody LaminaDTO dto) {

        return ResponseEntity.ok(service.put(id, dto));

    }

    @PatchMapping("/patch/{id}")
    public ResponseEntity<LaminaDTO> atualizarParcial(@PathVariable Long id, @RequestBody LaminaDTO dto) {

        return ResponseEntity.ok(service.patch(id, dto));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {

        service.deletar(id);
        return ResponseEntity.noContent().build();
        
    }
}