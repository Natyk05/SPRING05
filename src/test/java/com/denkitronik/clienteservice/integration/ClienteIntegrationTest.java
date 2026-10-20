package com.denkitronik.clienteservice.integration;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never",
                "spring.jpa.properties.hibernate.hbm2ddl.import_files="
        }
)
@AutoConfigureTestRestTemplate
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Execution(ExecutionMode.SAME_THREAD)
@DisplayName("E2E tests — flujo completo HTTP y PostgreSQL")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ClienteIntegrationTest {

    private static final String BASE = "/api/v1/cliente-service";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long regionId;
    private Long idCreado;

    @BeforeAll
    void prepararRegion() {
        regionId = jdbcTemplate.queryForObject(
                "INSERT INTO regiones(nombre) VALUES (?) RETURNING id",
                Long.class,
                "América del Sur"
        );
    }

    @Test
    @Order(1)
    @DisplayName("POST /clientes crea un cliente con estado 201")
    void crearCliente_debeRetornar201() {
        Cliente nuevo = construirCliente("Hamilton");

        ResponseEntity<Cliente> respuesta =
                restTemplate.postForEntity(
                        BASE + "/clientes",
                        nuevo,
                        Cliente.class
                );

        assertThat(respuesta.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();

        Cliente creado = respuesta.getBody();
        assertThat(creado.getId()).isNotNull();
        assertThat(creado.getNombre()).isEqualTo("Margaret");
        assertThat(creado.getCreateAt()).isNotNull();

        idCreado = creado.getId();
    }

    @Test
    @Order(2)
    @DisplayName("GET encuentra el cliente creado con estado 200")
    void buscarClienteCreado_debeRetornar200() {
        assertThat(idCreado).isNotNull();

        ResponseEntity<Cliente> respuesta =
                restTemplate.getForEntity(
                        BASE + "/clientes/" + idCreado,
                        Cliente.class
                );

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();

        Cliente encontrado = respuesta.getBody();
        assertThat(encontrado.getId()).isEqualTo(idCreado);
        assertThat(encontrado.getEmail())
                .isEqualTo("margaret@apollo.nasa");
        assertThat(encontrado.getRegion()).isNotNull();
        assertThat(encontrado.getRegion().getId()).isEqualTo(regionId);
        assertThat(encontrado.getRegion().getNombre())
                .isEqualTo("América del Sur");
    }

    @Test
    @Order(3)
    @DisplayName("GET /clientes incluye el cliente creado")
    void listarClientes_debeIncluirClienteCreado() {
        assertThat(idCreado).isNotNull();

        ResponseEntity<Cliente[]> respuesta =
                restTemplate.getForEntity(
                        BASE + "/clientes",
                        Cliente[].class
                );

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody())
                .extracting(Cliente::getId)
                .contains(idCreado);
    }

    @Test
    @Order(4)
    @DisplayName("GET /clientes/page/0 devuelve una página con clientes")
    void listarClientesPaginado_debeRetornarPagina() {
        assertThat(idCreado).isNotNull();

        ResponseEntity<Map> respuesta =
                restTemplate.getForEntity(
                        BASE + "/clientes/page/0",
                        Map.class
                );

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();

        Map<?, ?> pagina = respuesta.getBody();

        assertThat(pagina.get("totalElements"))
                .isInstanceOf(Number.class);
        assertThat(((Number) pagina.get("totalElements")).longValue())
                .isEqualTo(1L);

        assertThat(pagina.get("totalPages"))
                .isInstanceOf(Number.class);
        assertThat(((Number) pagina.get("totalPages")).intValue())
                .isEqualTo(1);

        assertThat(pagina.get("content")).isInstanceOf(List.class);

        List<?> contenido = (List<?>) pagina.get("content");
        assertThat(contenido).hasSize(1);
        assertThat(contenido.get(0)).isInstanceOf(Map.class);

        Map<?, ?> primerCliente = (Map<?, ?>) contenido.get(0);
        assertThat(primerCliente.get("id")).isInstanceOf(Number.class);
        assertThat(((Number) primerCliente.get("id")).longValue())
                .isEqualTo(idCreado.longValue());
    }

    @Test
    @Order(5)
    @DisplayName("POST con datos inválidos devuelve 400 y no guarda")
    void crearCliente_invalido_debeRetornar400SinGuardar() {
        Long cantidadAntes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clientes",
                Long.class
        );

        Cliente invalido = construirCliente("Hamilton");
        invalido.setNombre("");
        invalido.setEmail("correo-invalido");

        ResponseEntity<String> respuesta =
                restTemplate.postForEntity(
                        BASE + "/clientes",
                        invalido,
                        String.class
                );

        assertThat(respuesta.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        Long cantidadDespues = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clientes",
                Long.class
        );

        assertThat(cantidadDespues).isEqualTo(cantidadAntes);
    }

    @Test
    @Order(6)
    @DisplayName("PUT actualiza el cliente con estado 201")
    void actualizarCliente_debeRetornar201() {
        assertThat(idCreado).isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Cliente> peticion = new HttpEntity<>(
                construirCliente("Hamilton-Updated"),
                headers
        );

        ResponseEntity<Cliente> respuesta =
                restTemplate.exchange(
                        BASE + "/clientes/" + idCreado,
                        HttpMethod.PUT,
                        peticion,
                        Cliente.class
                );

        assertThat(respuesta.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getApellido())
                .isEqualTo("Hamilton-Updated");

        String apellidoGuardado = jdbcTemplate.queryForObject(
                "SELECT apellido FROM clientes WHERE id = ?",
                String.class,
                idCreado
        );

        assertThat(apellidoGuardado).isEqualTo("Hamilton-Updated");
    }

    @Test
    @Order(7)
    @DisplayName("GET de un ID inexistente devuelve 404")
    void idInexistente_debeRetornar404() {
        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(
                        BASE + "/clientes/9999",
                        String.class
                );

        assertThat(respuesta.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(8)
    @DisplayName("DELETE devuelve 204 y la consulta posterior devuelve 404")
    void eliminarCliente_debeRetornar204YLuego404() {
        assertThat(idCreado).isNotNull();

        ResponseEntity<Void> eliminacion =
                restTemplate.exchange(
                        BASE + "/clientes/" + idCreado,
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Void.class
                );

        assertThat(eliminacion.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(eliminacion.getBody()).isNull();

        ResponseEntity<String> consulta =
                restTemplate.getForEntity(
                        BASE + "/clientes/" + idCreado,
                        String.class
                );

        assertThat(consulta.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Cliente construirCliente(String apellido) {
        Region region = new Region();
        region.setId(regionId);

        Cliente cliente = new Cliente();
        cliente.setNombre("Margaret");
        cliente.setApellido(apellido);
        cliente.setEmail("margaret@apollo.nasa");
        cliente.setFoto("margaret.jpg");
        cliente.setRegion(region);

        return cliente;
    }
}