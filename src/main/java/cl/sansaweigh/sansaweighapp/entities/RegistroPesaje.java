package cl.sansaweigh.sansaweighapp.entities;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

//@Data
//@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "registros_pesajes")
public class RegistroPesaje implements Serializable {

    @Id
    private String id;

    @NotBlank(message = "Debe ingresar el ID de la balanza")
    private int balanzaId;

    @NotBlank(message = "Debe ingresar el ID del paquete")
    private String paqueteId;

    private Double pesoSansas;

    private CategoriaPeso categoria;

    private EstadoPesaje estado;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<String> historialTransiciones = new ArrayList<>();
}
