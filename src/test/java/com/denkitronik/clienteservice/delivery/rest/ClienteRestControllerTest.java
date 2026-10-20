package com.denkitronik.clienteservice.delivery.rest;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.services.IClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteRestController.class)
@DisplayName("Integration tests (web slice) — ClienteRestController")
class ClienteRestControllerTest {

    private static final String BASE = "/api/v1/cliente-service";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IClienteService clienteService;

    @Autowired
    private ObjectMapper objectMapper;

    private Cliente cliente;
    private Region region;

    @BeforeEach
    void setUp() {
        region = new Region();
        region.setId(4L);
        region.setNombre("Europa");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Ada");
        cliente.setApellido("Lovelace");
        cliente.setEmail("ada@babbage.uk");
        cliente.setFoto("ada.jpg");
        cliente.setRegion(region);
    }

    @Test
    @DisplayName("GET /clientes devuelve 200 y la lista de clientes")
    void listarClientes_debeRetornar200ConLista() throws Exception {
        when(clienteService.findAll()).thenReturn(List.of(cliente));

        mockMvc.perform(get(BASE + "/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ada"));

        verify(clienteService).findAll();
    }

    @Test
    @DisplayName("GET /clientes/1 devuelve 200 con el cliente")
    void buscarCliente_idExistente_debeRetornar200ConCliente()
            throws Exception {
        when(clienteService.findById(1L)).thenReturn(cliente);

        mockMvc.perform(get(BASE + "/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ada"))
                .andExpect(jsonPath("$.apellido").value("Lovelace"));

        verify(clienteService).findById(1L);
    }

    @Test
    @DisplayName("GET /clientes/999 devuelve 404")
    void buscarCliente_idInexistente_debeRetornar404()
            throws Exception {
        when(clienteService.findById(999L))
                .thenThrow(new ClienteNotFoundException(999L));

        mockMvc.perform(get(BASE + "/clientes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value(
                        "Cliente con id 999 no encontrado"
                ));
    }

    @Test
    @DisplayName("POST /clientes válido devuelve 201")
    void crearCliente_valido_debeRetornar201ConCliente()
            throws Exception {
        Cliente nuevo = new Cliente();
        nuevo.setNombre("Ada");
        nuevo.setApellido("Lovelace");
        nuevo.setEmail("ada@babbage.uk");
        nuevo.setFoto("ada.jpg");
        nuevo.setRegion(region);

        when(clienteService.save(any(Cliente.class)))
                .thenReturn(cliente);

        mockMvc.perform(post(BASE + "/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ada"))
                .andExpect(jsonPath("$.email").value("ada@babbage.uk"));

        verify(clienteService).save(any(Cliente.class));
    }

    @Test
    @DisplayName("POST /clientes con nombre vacío devuelve 400")
    void crearCliente_nombreVacio_debeRetornar400()
            throws Exception {
        cliente.setNombre("");

        mockMvc.perform(post(BASE + "/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors",
                        hasItem(containsString("nombre"))));

        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("POST /clientes con email inválido devuelve 400")
    void crearCliente_emailInvalido_debeRetornar400()
            throws Exception {
        cliente.setEmail("esto-no-es-un-email");

        mockMvc.perform(post(BASE + "/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors",
                        hasItem(containsString("email"))));

        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("PUT /clientes/1 válido devuelve 201")
    void actualizarCliente_valido_debeRetornar201()
            throws Exception {
        when(clienteService.update(eq(1L), any(Cliente.class)))
                .thenReturn(cliente);

        mockMvc.perform(put(BASE + "/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ada"));

        verify(clienteService).update(eq(1L), any(Cliente.class));
    }

    @Test
    @DisplayName("PUT /clientes/999 devuelve 404")
    void actualizarCliente_idInexistente_debeRetornar404()
            throws Exception {
        when(clienteService.update(eq(999L), any(Cliente.class)))
                .thenThrow(new ClienteNotFoundException(999L));

        mockMvc.perform(put(BASE + "/clientes/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /clientes/1 devuelve 204 sin contenido")
    void eliminarCliente_idExistente_debeRetornar204()
            throws Exception {
        mockMvc.perform(delete(BASE + "/clientes/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(clienteService).delete(1L);
    }

    @Test
    @DisplayName("DELETE /clientes/999 devuelve 404")
    void eliminarCliente_idInexistente_debeRetornar404()
            throws Exception {
        doThrow(new ClienteNotFoundException(999L))
                .when(clienteService).delete(999L);

        mockMvc.perform(delete(BASE + "/clientes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(clienteService).delete(999L);
    }

    @Test
    @DisplayName("GET /clientes/regiones devuelve 200 y las regiones")
    void listarRegiones_debeRetornar200ConLista()
            throws Exception {
        when(clienteService.findAllRegiones()).thenReturn(List.of(region));

        mockMvc.perform(get(BASE + "/clientes/regiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[0].nombre").value("Europa"));

        verify(clienteService).findAllRegiones();
    }
}