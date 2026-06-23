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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sa.smart.dto.EstoqueDTO;
import com.sa.smart.service.EstoqueService;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    private final EstoqueService service;

    public EstoqueController(EstoqueService service) {
        this.service = service;
    }

    @PostMapping("/salvar")
    public ResponseEntity<EstoqueDTO> criar(@RequestBody EstoqueDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @GetMapping("/listar")
    public ResponseEntity<List<EstoqueDTO>> listar() {
        List<EstoqueDTO> lista = service.listar();
        if (lista.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(lista);
    }

    // CORRIGIDO: URL era "/listar/disponivel" sem {id}, mas o método recebia @PathVariable Long id.
    // Separado em dois endpoints distintos:

    /** Busca um estoque pelo ID. */
    @GetMapping("/listarId/{id}")
    public ResponseEntity<EstoqueDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    /** Lista apenas as posições disponíveis (cor = 0). */
    @GetMapping("/disponiveis")
    public ResponseEntity<List<EstoqueDTO>> listarDisponiveis() {
        List<EstoqueDTO> lista = service.listar().stream()
                .filter(e -> e.cor() != null && e.cor() == 0)
                .toList();
        if (lista.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/put/{id}")
    public ResponseEntity<EstoqueDTO> atualizarTotal(
            @PathVariable Long id, @RequestBody EstoqueDTO dto) {
        return ResponseEntity.ok(service.put(id, dto));
    }

    @PatchMapping("/patch/{id}")
    public ResponseEntity<EstoqueDTO> atualizarParcial(
            @PathVariable Long id, @RequestBody EstoqueDTO dto) {
        return ResponseEntity.ok(service.patch(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}