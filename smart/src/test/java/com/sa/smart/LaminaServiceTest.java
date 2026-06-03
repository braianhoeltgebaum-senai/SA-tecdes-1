package com.sa.smart;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


import java.util.List;
import java.util.Optional;


import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import com.sa.smart.dto.LaminaDTO;
import com.sa.smart.model.Bloco;
import com.sa.smart.model.Lamina;
import com.sa.smart.repository.BlocoRepository;
import com.sa.smart.repository.LaminaRepository;
import com.sa.smart.service.LaminaService;


@ExtendWith(MockitoExtension.class)
class LaminaServiceTest {


    @Mock
    private LaminaRepository laminaRepository;


    @Mock
    private BlocoRepository blocoRepository;


    @Mock
    private EntityManager entityManager;


    @Mock
    private TypedQuery<Long> typedQuery;


    @InjectMocks
    private LaminaService laminaService;


    private Lamina lamina;
    private Bloco bloco;
    private LaminaDTO dto;


    @BeforeEach
    void setup() {


        bloco = new Bloco();
        bloco.setIdBloco(1L);


        lamina = new Lamina();
        lamina.setId(1L);
        lamina.setCor(1);
        lamina.setPadrao(5);
        lamina.setPosicaoNoBloco(1);
        lamina.setBloco(bloco);


        dto = new LaminaDTO(
                null,
                1,
                5,
                1,
                1L);
    }


    private void mockContagemLaminas(long quantidade) {


        when(entityManager.createQuery(anyString(), eq(Long.class)))
                .thenReturn(typedQuery);


        when(typedQuery.setParameter(eq("idBloco"), any()))
                .thenReturn(typedQuery);


        when(typedQuery.getSingleResult())
                .thenReturn(quantidade);
    }


    @Test
    void deveCriarLamina() {


        when(blocoRepository.findById(1L))
                .thenReturn(Optional.of(bloco));


        mockContagemLaminas(1);


        when(laminaRepository.save(any(Lamina.class)))
                .thenReturn(lamina);


        LaminaDTO resultado = laminaService.criar(dto);


        assertThat(resultado).isNotNull();
        assertThat(resultado.cor()).isEqualTo(1);


        verify(laminaRepository)
                .save(any(Lamina.class));
    }


    @Test
    void deveListarLaminas() {


        when(laminaRepository.findAll())
                .thenReturn(List.of(lamina));


        List<LaminaDTO> lista = laminaService.listar();


        assertThat(lista).hasSize(1);
    }


    @Test
    void deveBuscarLaminaPorId() {


        when(laminaRepository.findById(1L))
                .thenReturn(Optional.of(lamina));


        LaminaDTO resultado = laminaService.buscar(1L);


        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
    }


    @Test
    void deveAtualizarLaminaComPut() {


        LaminaDTO atualizado = new LaminaDTO(
                null,
                2,
                7,
                2,
                1L);


        when(laminaRepository.findById(1L))
                .thenReturn(Optional.of(lamina));


        when(blocoRepository.findById(1L))
                .thenReturn(Optional.of(bloco));


        when(laminaRepository.save(any(Lamina.class)))
                .thenReturn(lamina);


        LaminaDTO resultado = laminaService.put(1L, atualizado);


        assertThat(resultado).isNotNull();


        verify(laminaRepository)
                .save(any(Lamina.class));
    }


    @Test
    void deveAtualizarLaminaComPatch() {


        LaminaDTO patch = new LaminaDTO(
                null,
                3,
                8,
                2,
                null);


        when(laminaRepository.findById(1L))
                .thenReturn(Optional.of(lamina));


        when(laminaRepository.save(any(Lamina.class)))
                .thenReturn(lamina);


        LaminaDTO resultado = laminaService.patch(1L, patch);


        assertThat(resultado).isNotNull();


        verify(laminaRepository)
                .save(any(Lamina.class));
    }


    @Test
    void deveDeletarLamina() {


        when(laminaRepository.existsById(1L))
                .thenReturn(true);


        doNothing().when(laminaRepository)
                .deleteById(1L);


        laminaService.deletar(1L);


        verify(laminaRepository)
                .deleteById(1L);
    }


    @Test
    void deveLancarErroQuandoLaminaNaoExisteAoBuscar() {


        when(laminaRepository.findById(1L))
                .thenReturn(Optional.empty());


        assertThatThrownBy(() -> laminaService.buscar(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lâmina não encontrada");
    }


    @Test
    void deveLancarErroQuandoLaminaNaoExisteAoDeletar() {


        when(laminaRepository.existsById(1L))
                .thenReturn(false);


        assertThatThrownBy(() -> laminaService.deletar(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Lâmina não encontrada");
    }


    @Test
    void deveLancarErroQuandoBlocoNaoExiste() {


        when(blocoRepository.findById(1L))
                .thenReturn(Optional.empty());


        assertThatThrownBy(() -> laminaService.criar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bloco não encontrado");
    }


    @Test
    void deveLancarErroQuandoBlocoJaPossuiTresLaminas() {


        when(blocoRepository.findById(1L))
                .thenReturn(Optional.of(bloco));


        mockContagemLaminas(3);


        assertThatThrownBy(() -> laminaService.criar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("3 lâminas");
    }


    @Test
    void deveLancarErroQuandoPosicaoForMaiorQueTres() {


        LaminaDTO dtoInvalido = new LaminaDTO(
                null,
                1,
                5,
                4,
                1L);


        when(blocoRepository.findById(1L))
                .thenReturn(Optional.of(bloco));


        assertThatThrownBy(() -> laminaService.criar(dtoInvalido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("entre 1 e 3");
    }


    @Test
    void deveLancarErroQuandoPosicaoForMenorQueUm() {


        LaminaDTO dtoInvalido = new LaminaDTO(
                null,
                1,
                5,
                0,
                1L);


        when(blocoRepository.findById(1L))
                .thenReturn(Optional.of(bloco));


        assertThatThrownBy(() -> laminaService.criar(dtoInvalido))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("entre 1 e 3");
    }
}
