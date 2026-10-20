package com.denkitronik.clienteservice.domain.services;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import com.denkitronik.clienteservice.domain.repositories.IClienteDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests — ClienteServiceImpl con Mockito")
class ClienteServiceImplTest {

    @Mock
    private IClienteDao clienteDao;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private Cliente cliente;
    private Region region;

    @BeforeEach
    void setUp() {
        region = new Region();
        region.setId(1L);
        region.setNombre("Sudamérica");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Linus");
        cliente.setApellido("Torvalds");
        cliente.setEmail("linus@kernel.org");
        cliente.setFoto("linus.jpg");
        cliente.setRegion(region);
    }

    @Test
    @DisplayName("findAll — devuelve la lista de clientes")
    void findAll_debeRetornarClientes() {
        // Arrange
        when(clienteDao.findAll()).thenReturn(List.of(cliente));

        // Act
        List<Cliente> resultado = clienteService.findAll();

        // Assert
        assertThat(resultado).containsExactly(cliente);
        verify(clienteDao).findAll();
    }

    @Test
    @DisplayName("findAll — devuelve la página solicitada")
    void findAll_conPaginacion_debeRetornarPagina() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 4);
        Page<Cliente> pagina =
                new PageImpl<>(List.of(cliente), pageable, 1);

        when(clienteDao.findAll(pageable)).thenReturn(pagina);

        // Act
        Page<Cliente> resultado = clienteService.findAll(pageable);

        // Assert
        assertThat(resultado.getContent()).containsExactly(cliente);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(clienteDao).findAll(pageable);
    }

    @Test
    @DisplayName("findById — ID existente devuelve el cliente")
    void findById_idExistente_debeRetornarCliente() {
        // Arrange
        when(clienteDao.findById(1L))
                .thenReturn(Optional.of(cliente));

        // Act
        Cliente resultado = clienteService.findById(1L);

        // Assert
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Linus");
        verify(clienteDao).findById(1L);
    }

    @Test
    @DisplayName("findById — ID inexistente lanza excepción")
    void findById_idInexistente_debeLanzarExcepcion() {
        // Arrange
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> clienteService.findById(999L))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("save — guarda y devuelve el cliente")
    void save_clienteValido_debeGuardarYRetornar() {
        // Arrange
        when(clienteDao.save(cliente)).thenReturn(cliente);

        // Act
        Cliente resultado = clienteService.save(cliente);

        // Assert
        assertThat(resultado).isSameAs(cliente);
        verify(clienteDao).save(cliente);
    }

    @Test
    @DisplayName("save — email duplicado lanza ClienteServiceException")
    void save_emailDuplicado_debeLanzarClienteServiceException() {
        // Arrange
        when(clienteDao.save(cliente))
                .thenThrow(
                        new DataIntegrityViolationException("duplicate key")
                );

        // Act + Assert
        assertThatThrownBy(() -> clienteService.save(cliente))
                .isInstanceOf(ClienteServiceException.class)
                .hasMessageContaining("duplicados")
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("delete — ID existente elimina el cliente")
    void delete_idExistente_debeEliminar() {
        // Arrange
        when(clienteDao.existsById(1L)).thenReturn(true);

        // Act
        clienteService.delete(1L);

        // Assert
        verify(clienteDao).existsById(1L);
        verify(clienteDao).deleteById(1L);
    }

    @Test
    @DisplayName("delete — ID inexistente no elimina")
    void delete_idInexistente_noDebeEliminar() {
        // Arrange
        when(clienteDao.existsById(999L)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> clienteService.delete(999L))
                .isInstanceOf(ClienteNotFoundException.class);

        verify(clienteDao, never()).deleteById(any());
    }

    @Test
    @DisplayName("update — ID existente actualiza el cliente")
    void update_idExistente_debeActualizarYRetornar() {
        // Arrange
        Region nuevaRegion = new Region();
        nuevaRegion.setId(2L);
        nuevaRegion.setNombre("Europa");

        Cliente datosNuevos = new Cliente();
        datosNuevos.setNombre("Linus Updated");
        datosNuevos.setApellido("Torvalds Updated");
        datosNuevos.setEmail("linus2@kernel.org");
        datosNuevos.setFoto("actualizada.jpg");
        datosNuevos.setRegion(nuevaRegion);

        when(clienteDao.findById(1L))
                .thenReturn(Optional.of(cliente));

        when(clienteDao.saveAndFlush(any(Cliente.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        Cliente resultado = clienteService.update(1L, datosNuevos);

        // Assert
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Linus Updated");
        assertThat(resultado.getApellido()).isEqualTo("Torvalds Updated");
        assertThat(resultado.getEmail()).isEqualTo("linus2@kernel.org");
        assertThat(resultado.getFoto()).isEqualTo("actualizada.jpg");
        assertThat(resultado.getRegion()).isSameAs(nuevaRegion);

        verify(clienteDao).findById(1L);
        verify(clienteDao).saveAndFlush(cliente);
    }

    @Test
    @DisplayName("update — ID inexistente lanza excepción")
    void update_idInexistente_debeLanzarExcepcion() {
        // Arrange
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> clienteService.update(999L, cliente))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");

        verify(clienteDao, never()).saveAndFlush(any(Cliente.class));
    }

    @Test
    @DisplayName("findAllRegiones — devuelve las regiones")
    void findAllRegiones_debeRetornarRegiones() {
        // Arrange
        when(clienteDao.findAllRegiones()).thenReturn(List.of(region));

        // Act
        List<Region> resultado = clienteService.findAllRegiones();

        // Assert
        assertThat(resultado).containsExactly(region);
        verify(clienteDao).findAllRegiones();
    }
}