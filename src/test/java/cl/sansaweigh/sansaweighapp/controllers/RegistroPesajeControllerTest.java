package cl.sansaweigh.sansaweighapp.controllers;

import cl.sansaweigh.sansaweighapp.dto.RegistroPesajeResponse;
import cl.sansaweigh.sansaweighapp.entities.CategoriaPeso;
import cl.sansaweigh.sansaweighapp.entities.EstadoPesaje;
import cl.sansaweigh.sansaweighapp.exceptions.IllegalWeighingStateException;
import cl.sansaweigh.sansaweighapp.exceptions.RegistroNoEncontradoException;
import cl.sansaweigh.sansaweighapp.services.RegistroPesajeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistroPesajeController.class)
class RegistroPesajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistroPesajeService registroPesajeService;

    private RegistroPesajeResponse sampleResponse(EstadoPesaje estado) {
        return new RegistroPesajeResponse(
                "id1", 101, "PKG-1", 7.48,
                CategoriaPeso.LIVIANO, estado,
                LocalDateTime.now(), LocalDateTime.now(),
                List.of("Creado en estado INGRESADO"));
    }

    @Test
    void createRegistro_devuelve201() throws Exception {
        when(registroPesajeService.createRegistro(any())).thenReturn(sampleResponse(EstadoPesaje.INGRESADO));

        mockMvc.perform(post("/registros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balanzaId\":101,\"paqueteId\":\"PKG-1\",\"pesoKg\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value("LIVIANO"))
                .andExpect(jsonPath("$.estado").value("INGRESADO"));
    }

    @Test
    void createRegistro_requestInvalido_devuelve400() throws Exception {
        // falta paqueteId y pesoKg negativo
        mockMvc.perform(post("/registros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balanzaId\":101,\"pesoKg\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEstado_put_devuelve200() throws Exception {
        when(registroPesajeService.updateEstado(eq("id1"), any())).thenReturn(sampleResponse(EstadoPesaje.PESADO));

        mockMvc.perform(put("/registros/id1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"PESADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PESADO"));
    }

    @Test
    void updateEstado_patch_devuelve200() throws Exception {
        when(registroPesajeService.updateEstado(eq("id1"), any())).thenReturn(sampleResponse(EstadoPesaje.PESADO));

        mockMvc.perform(patch("/registros/id1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"PESADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PESADO"));
    }

    @Test
    void updateEstado_transicionInvalida_devuelve400() throws Exception {
        when(registroPesajeService.updateEstado(anyString(), any()))
                .thenThrow(new IllegalWeighingStateException("Transición inválida"));

        mockMvc.perform(put("/registros/id1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"DESPACHADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateEstado_noEncontrado_devuelve404() throws Exception {
        when(registroPesajeService.updateEstado(anyString(), any()))
                .thenThrow(new RegistroNoEncontradoException("Registro no encontrado con ID: noexiste"));

        mockMvc.perform(put("/registros/noexiste/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"PESADO\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getRegistrosByFecha_devuelve200() throws Exception {
        when(registroPesajeService.getRegistrosByFecha(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/registros")
                        .param("desde", "2026-06-01T00:00:00")
                        .param("hasta", "2026-06-30T23:59:59"))
                .andExpect(status().isOk());
    }
}
