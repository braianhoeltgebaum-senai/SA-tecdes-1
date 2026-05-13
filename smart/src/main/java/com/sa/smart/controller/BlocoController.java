package com.sa.smart.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    public ResponseEntity<List<BlocoDTO>> listarPedidos() {

        List<Bloco> blocos = blocoService.listarTodos();
        List<BlocoDTO> blocosDTO = blocos.stream()
            .map(BlocoDTO::fromEntity)
            .collect(Collectors.toList());

        return ResponseEntity.ok(blocosDTO);

    }
    
    @PostMapping("/pedidos")
    public ResponseEntity<BlocoDTO> criarPedido(@RequestBody BlocoDTO blocoDTO) {

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

    }

    @GetMapping("/estoque/disponivel")
    public ResponseEntity<List<BlocoDTO>> listarEstoqueDisponivel() {

        List<Bloco> blocosDisponiveis = blocoService.listarBlocosDisponiveis();
        List<BlocoDTO> blocosDTO = blocosDisponiveis.stream()
            .map(BlocoDTO::fromEntity)
            .collect(Collectors.toList());

        return ResponseEntity.ok(blocosDTO);

    }
    
    @PutMapping("/pedidos/{id}/status")
    public ResponseEntity<Void> atualizarStatus(@PathVariable Integer id) {

        blocoService.atualizarStatusConcluido(id);
        return ResponseEntity.ok().build();

    }
    
}
