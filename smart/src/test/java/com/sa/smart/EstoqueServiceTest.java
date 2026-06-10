// package com.sa.smart;


// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;


// import java.util.List;
// import java.util.Optional;


// import jakarta.persistence.EntityNotFoundException;


// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;


// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;


// import com.sa.smart.dto.EstoqueDTO;
// import com.sa.smart.model.Estoque;
// import com.sa.smart.repository.EstoqueRepository;
// import com.sa.smart.service.EstoqueService;


// @ExtendWith(MockitoExtension.class)
// class EstoqueServiceTest {


//     @Mock
//     private EstoqueRepository repository;


//     @InjectMocks
//     private EstoqueService estoqueService;


//     private Estoque estoque;
//     private EstoqueDTO dto;


//     @BeforeEach
//     void setup() {


//         estoque = new Estoque();
//         estoque.setId(1L);
//         estoque.setPosicao(10);
//         estoque.setCor(1);


//         dto = new EstoqueDTO(
//                 null,
//                 10,
//                 1
//         );
//     }


//     @Test
//     void deveCriarEstoque() {


//         when(repository.save(any(Estoque.class)))
//                 .thenReturn(estoque);


//         EstoqueDTO resultado = estoqueService.criar(dto);


//         assertThat(resultado).isNotNull();
//         assertThat(resultado.posicao()).isEqualTo(10);
//         assertThat(resultado.cor()).isEqualTo(1);


//         verify(repository, times(1))
//                 .save(any(Estoque.class));
//     }


//     @Test
//     void deveListarEstoques() {


//         when(repository.findAll())
//                 .thenReturn(List.of(estoque));


//         List<EstoqueDTO> lista = estoqueService.listar();


//         assertThat(lista).hasSize(1);
//         assertThat(lista.get(0).posicao()).isEqualTo(10);
//     }


//     @Test
//     void deveBuscarEstoquePorId() {


//         when(repository.findById(1L))
//                 .thenReturn(Optional.of(estoque));


//         EstoqueDTO resultado = estoqueService.buscar(1L);


//         assertThat(resultado).isNotNull();
//         assertThat(resultado.id()).isEqualTo(1L);
//         assertThat(resultado.posicao()).isEqualTo(10);
//     }


//     @Test
//     void deveAtualizarEstoqueComPut() {


//         EstoqueDTO atualizado = new EstoqueDTO(
//                 null,
//                 20,
//                 2
//         );


//         when(repository.findById(1L))
//                 .thenReturn(Optional.of(estoque));


//         when(repository.save(any(Estoque.class)))
//                 .thenReturn(estoque);


//         EstoqueDTO resultado = estoqueService.put(1L, atualizado);


//         assertThat(resultado).isNotNull();


//         verify(repository)
//                 .save(any(Estoque.class));
//     }


//     @Test
//     void deveAtualizarEstoqueComPatch() {


//         EstoqueDTO patch = new EstoqueDTO(
//                 null,
//                 99,
//                 null
//         );


//         when(repository.findById(1L))
//                 .thenReturn(Optional.of(estoque));


//         when(repository.save(any(Estoque.class)))
//                 .thenReturn(estoque);


//         EstoqueDTO resultado = estoqueService.patch(1L, patch);


//         assertThat(resultado).isNotNull();


//         verify(repository)
//                 .save(any(Estoque.class));
//     }


//     @Test
//     void deveDeletarEstoque() {


//         when(repository.existsById(1L))
//                 .thenReturn(true);


//         doNothing().when(repository)
//                 .deleteById(1L);


//         estoqueService.deletar(1L);


//         verify(repository)
//                 .deleteById(1L);
//     }


//     @Test
//     void deveLancarErroQuandoEstoqueNaoExisteAoBuscar() {


//         when(repository.findById(1L))
//                 .thenReturn(Optional.empty());


//         assertThatThrownBy(() -> estoqueService.buscar(1L))
//                 .isInstanceOf(RuntimeException.class)
//                 .hasMessageContaining("Estoque não encontrado");
//     }


//     @Test
//     void deveLancarErroQuandoEstoqueNaoExisteAoDeletar() {


//         when(repository.existsById(1L))
//                 .thenReturn(false);


//         assertThatThrownBy(() -> estoqueService.deletar(1L))
//                 .isInstanceOf(EntityNotFoundException.class)
//                 .hasMessageContaining("Estoque não encontrado");
//     }
// }
