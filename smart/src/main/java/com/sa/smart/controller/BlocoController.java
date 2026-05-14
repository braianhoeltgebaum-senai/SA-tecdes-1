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
import com.sa.smart.dto.BlocoDTO;
import com.sa.smart.service.BlocoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bloco")
@RequiredArgsConstructor
public class BlocoController {

    private final BlocoService blocoService;

    @PostMapping("/salvar")
    public ResponseEntity<BlocoDTO> criar(@RequestBody BlocoDTO dto) {
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(blocoService.criar(dto));

    }

    @GetMapping("/listar")
    public ResponseEntity<List<BlocoDTO>> listar() {

        List<BlocoDTO> lista = blocoService.listar();
        if (lista.isEmpty()) return ResponseEntity.noContent().build();

        return ResponseEntity.ok(lista);

    }

    @GetMapping("/listarId/{id}")
    public ResponseEntity<BlocoDTO> buscar(@PathVariable Long id) {

        return ResponseEntity.ok(blocoService.buscar(id));

    }

    @PutMapping("/put/{id}")
    public ResponseEntity<BlocoDTO> atualizarTotal(@PathVariable Long id, @RequestBody BlocoDTO dto) {

        return ResponseEntity.ok(blocoService.put(id, dto));

    }

    @PatchMapping("/patch/{id}")
    public ResponseEntity<BlocoDTO> atualizarParcial(@PathVariable Long id, @RequestBody BlocoDTO dto) {

        return ResponseEntity.ok(blocoService.patch(id, dto));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {

        blocoService.deletar(id);
        return ResponseEntity.noContent().build();
        
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<List<BlocoDTO>> listarDisponiveis() {

        List<BlocoDTO> lista = blocoService.listarBlocosDisponiveis();
        if (lista.isEmpty()) return ResponseEntity.noContent().build();

        return ResponseEntity.ok(lista);

    }

    @PutMapping("/{id}/status-concluido")
    public ResponseEntity<Void> atualizarStatusConcluido(@PathVariable Long id) {

        blocoService.atualizarStatusConcluido(id);
        return ResponseEntity.ok().build();

    }
}