package cl.sansaweigh.sansaweighapp.controllers;

import cl.sansaweigh.sansaweighapp.entities.ScaleSpecification;
import cl.sansaweigh.sansaweighapp.services.ScaleSpecificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScaleSpecificationController.class)
class ScaleSpecificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScaleSpecificationService scaleSpecificationService;

    @Test
    void getSpecs_devuelve200ConLaBalanza() throws Exception {
        ScaleSpecification spec = new ScaleSpecification(
                "101", "Balanza Central Sur", "SansaScale-Pro", 150.0, 0.01, -0.0);
        when(scaleSpecificationService.getScaleSpecification("101")).thenReturn(spec);

        mockMvc.perform(get("/balanzas/101/specs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("101"))
                .andExpect(jsonPath("$.name").value("Balanza Central Sur"));
    }
}
