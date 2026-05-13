package com.sa.smart.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sa.smart.dto.BlocoDTO;
import com.sa.smart.model.Bloco;
import com.sa.smart.model.Estoque;
import com.sa.smart.service.BlocoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BlocoController {

    private final BlocoService blocoService;

    @GetMapping("/pedidos")
    public ResponseEntity<?> listarPedidos() {

        try {

            List<Bloco> blocos = blocoService.listarTodos();
            List<BlocoDTO> blocosDTO = new ArrayList<>();

            for (Bloco bloco : blocos) {

                blocosDTO.add(BlocoDTO.fromEntity(bloco));

            }

            return ResponseEntity.ok(blocosDTO);

        } catch (RuntimeException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        }
    }

    @PostMapping("/pedidos")
    public ResponseEntity<?> criarPedido(@RequestBody BlocoDTO blocoDTO) {

        try {

            Bloco bloco = new Bloco();

            if (blocoDTO.getPedidoOrdemProducao() != null) {

                Pedido pedido = new Pedido();
                pedido.setOrdemProducao(blocoDTO.getPedidoOrdemProducao());
                bloco.setPedido(pedido);

            }

            if (blocoDTO.getEstoquePosicao() != null) {

                Estoque estoque = new Estoque();
                estoque.setPosicao(blocoDTO.getEstoquePosicao());
                bloco.setEstoque(estoque);

            }

            bloco.setCorBloco(blocoDTO.getCorBloco());

            Bloco blocoSalvo = blocoService.salvarBloco(bloco);
            return ResponseEntity.status(HttpStatus.CREATED).body(BlocoDTO.fromEntity(blocoSalvo));

        } catch (RuntimeException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        }
    }

    @GetMapping("/estoque/disponivel")
    public ResponseEntity<?> listarEstoqueDisponivel() {

        try {

            List<Bloco> blocosDisponiveis = blocoService.listarBlocosDisponiveis();
            List<BlocoDTO> blocosDTO = new ArrayList<>();

            for (Bloco bloco : blocosDisponiveis) {

                blocosDTO.add(BlocoDTO.fromEntity(bloco));

            }

            return ResponseEntity.ok(blocosDTO);

        } catch (RuntimeException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        }
    }

    @PutMapping("/pedidos/{id}/status")
    public ResponseEntity<?> atualizarStatus(@PathVariable Long id) {

        try {

            blocoService.atualizarStatusConcluido(id);
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        }
    }
}