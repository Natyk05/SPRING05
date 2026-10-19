package com.denkitronik.clienteservice.delivery.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica los requisitos del reto del Codelab 04 contra H2 en memoria.
 * Cada prueba corre en una transacción que se revierte al final, así que
 * los datos de import.sql quedan intactos entre pruebas.
 */
@SpringBootTest
@Transactional
class ClienteRestControllerRetoTest {

    private static final String BASE = "/api/v1/cliente-service/clientes";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ── GET ───────────────────────────────────────────────────
    @Test
    void getClienteExistente_responde200() throws Exception {
        mvc.perform(get(BASE + "/1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.nombre").value("Linus"));
    }

    @Test
    void getClienteInexistente_responde404ConApiErrorResponse() throws Exception {
        mvc.perform(get(BASE + "/999"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.status").value(404))
           .andExpect(jsonPath("$.error").value("Not Found"))
           .andExpect(jsonPath("$.mensaje").value("Cliente con id 999 no encontrado"))
           .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── DELETE ────────────────────────────────────────────────
    @Test
    void deleteClienteExistente_responde204SinBody() throws Exception {
        mvc.perform(delete(BASE + "/12"))
           .andExpect(status().isNoContent())
           .andExpect(content().string(""));

        mvc.perform(get(BASE + "/12"))
           .andExpect(status().isNotFound());
    }

    @Test
    void deleteClienteInexistente_responde404ConApiErrorResponse() throws Exception {
        mvc.perform(delete(BASE + "/999"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.status").value(404))
           .andExpect(jsonPath("$.error").value("Not Found"))
           .andExpect(jsonPath("$.mensaje").value("Cliente con id 999 no encontrado"))
           .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── POST ──────────────────────────────────────────────────
    @Test
    void postValidoSoloConCamposDeLaEntidad_responde201ConIdYCreateAt() throws Exception {
        String body = """
            {"nombre":"Donald","apellido":"Knuth","email":"knuth@stanford.edu","foto":"knuth.jpg"}
            """;
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").exists())
           .andExpect(jsonPath("$.createAt").exists())
           .andExpect(jsonPath("$.email").value("knuth@stanford.edu"));
    }

    @Test
    void postValidoConRegion_responde201() throws Exception {
        String body = """
            {"nombre":"Barbara","apellido":"Liskov","email":"liskov@mit.edu","foto":"b.jpg","region":{"id":3}}
            """;
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void postInvalido_responde400() throws Exception {
        String body = """
            {"nombre":"","apellido":"","email":"no-email","foto":""}
            """;
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void postEmailDuplicado_responde500GenericoSinDetallesInternos() throws Exception {
        String body = """
            {"nombre":"Otro","apellido":"Gosling","email":"james@java.com","foto":"x.jpg"}
            """;
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.status").value(500))
           .andExpect(jsonPath("$.error").value("Internal Server Error"))
           .andExpect(jsonPath("$.mensaje").value("Error interno del servidor. Por favor intenta más tarde."));
    }

    // ── PUT ───────────────────────────────────────────────────
    @Test
    void putClienteExistente_responde201YPreservaCreateAt() throws Exception {
        String body = """
            {"nombre":"Linus","apellido":"Benedict Torvalds","email":"linus@kernel.org","foto":"linus-updated.jpg"}
            """;
        mvc.perform(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.apellido").value("Benedict Torvalds"))
           .andExpect(jsonPath("$.email").value("linus@kernel.org"));
    }

    @Test
    void putClienteInexistente_responde404ConApiErrorResponse() throws Exception {
        String body = """
            {"nombre":"Fantasma","apellido":"Digital","email":"fantasma@no.com","foto":"f.jpg"}
            """;
        mvc.perform(put(BASE + "/999").contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.status").value(404))
           .andExpect(jsonPath("$.mensaje").value("Cliente con id 999 no encontrado"));
    }

    // ── Paginación ────────────────────────────────────────────
    @Test
    void paginaCero_traeCuatroClientes() throws Exception {
        mvc.perform(get(BASE + "/page/0"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content.length()").value(4));
    }
}
