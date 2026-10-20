package com.denkitronik.clienteservice.domain.repositories;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("Integration tests (data slice) — IClienteDao con H2")
class IClienteDaoTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private IClienteDao clienteDao;

    private Region region;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        region = new Region();
        region.setNombre("Asia");
        em.persist(region);

        cliente = new Cliente();
        cliente.setNombre("Grace");
        cliente.setApellido("Hopper");
        cliente.setEmail("grace@navy.mil");
        cliente.setFoto("grace.jpg");
        cliente.setRegion(region);
        em.persist(cliente);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findById — encuentra un cliente persistido")
    void findById_idExistente_debeRetornarCliente() {
        // Act
        Optional<Cliente> resultado =
                clienteDao.findById(cliente.getId());

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.orElseThrow().getNombre())
                .isEqualTo("Grace");
        assertThat(resultado.orElseThrow().getEmail())
                .isEqualTo("grace@navy.mil");
        assertThat(resultado.orElseThrow().getRegion().getNombre())
                .isEqualTo("Asia");
    }

    @Test
    @DisplayName("save — guarda un cliente y genera ID y fecha")
    void save_clienteNuevo_debePersistirConIdYFecha() {
        // Arrange
        Cliente nuevo = new Cliente();
        nuevo.setNombre("Ada");
        nuevo.setApellido("Lovelace");
        nuevo.setEmail("ada@example.com");
        nuevo.setFoto("ada.jpg");
        nuevo.setRegion(em.find(Region.class, region.getId()));

        // Act
        Cliente guardado = clienteDao.save(nuevo);
        em.flush();
        Long id = guardado.getId();
        em.clear();

        // Assert
        assertThat(id).isNotNull();

        Cliente persistido = em.find(Cliente.class, id);
        assertThat(persistido).isNotNull();
        assertThat(persistido.getNombre()).isEqualTo("Ada");
        assertThat(persistido.getEmail()).isEqualTo("ada@example.com");
        assertThat(persistido.getCreateAt()).isNotNull();
        assertThat(persistido.getRegion().getId())
                .isEqualTo(region.getId());
    }

    @Test
    @DisplayName("findAll — devuelve los clientes paginados")
    void findAll_conPaginacion_debeRetornarPagina() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 4);

        // Act
        Page<Cliente> resultado = clienteDao.findAll(pageable);

        // Assert
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getNumber()).isZero();
        assertThat(resultado.getSize()).isEqualTo(4);
        assertThat(resultado.getContent())
                .extracting(Cliente::getEmail)
                .containsExactly("grace@navy.mil");
    }

    @Test
    @DisplayName("saveAndFlush — actualiza un cliente existente")
    void saveAndFlush_clienteExistente_debeActualizar() {
        // Arrange
        Long id = cliente.getId();
        Cliente existente = em.find(Cliente.class, id);
        existente.setNombre("Grace Updated");
        existente.setEmail("grace.updated@example.com");

        // Act
        clienteDao.saveAndFlush(existente);
        em.clear();

        // Assert
        Cliente actualizado = em.find(Cliente.class, id);
        assertThat(actualizado).isNotNull();
        assertThat(actualizado.getNombre()).isEqualTo("Grace Updated");
        assertThat(actualizado.getEmail())
                .isEqualTo("grace.updated@example.com");
    }

    @Test
    @DisplayName("findAllRegiones — devuelve las regiones persistidas")
    void findAllRegiones_debeRetornarListaDeRegiones() {
        // Act
        List<Region> regiones = clienteDao.findAllRegiones();

        // Assert
        assertThat(regiones).hasSize(1);
        assertThat(regiones.get(0).getId()).isEqualTo(region.getId());
        assertThat(regiones.get(0).getNombre()).isEqualTo("Asia");
    }

    @Test
    @DisplayName("deleteById — elimina el cliente de la base de datos")
    void deleteById_debeEliminarElCliente() {
        // Arrange
        Long id = cliente.getId();

        // Act
        clienteDao.deleteById(id);
        em.flush();
        em.clear();

        // Assert
        assertThat(em.find(Cliente.class, id)).isNull();
        assertThat(em.find(Region.class, region.getId())).isNotNull();
    }
}