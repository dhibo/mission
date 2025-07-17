package tn.esprit.tpfoyer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.tpfoyer.control.FoyerRestController;
import tn.esprit.tpfoyer.entity.Foyer;
import tn.esprit.tpfoyer.service.IFoyerService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FoyerRestControllerTest {

    @Mock
    private IFoyerService foyerService;

    @InjectMocks
    private FoyerRestController foyerRestController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Foyer foyer1;
    private Foyer foyer2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(foyerRestController).build();
        objectMapper = new ObjectMapper();

        // Initialisation des données de test
        foyer1 = new Foyer();
        foyer1.setIdFoyer(1L);
        foyer1.setNomFoyer("Foyer A");
        foyer1.setCapaciteFoyer(100);

        foyer2 = new Foyer();
        foyer2.setIdFoyer(2L);
        foyer2.setNomFoyer("Foyer B");
        foyer2.setCapaciteFoyer(150);
    }

    @Test
    void testGetFoyers() throws Exception {
        // Arrange
        List<Foyer> foyers = Arrays.asList(foyer1, foyer2);
        when(foyerService.retrieveAllFoyers()).thenReturn(foyers);

        // Act & Assert
        mockMvc.perform(get("/foyer/retrieve-all-foyers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].idFoyer").value(1))
                .andExpect(jsonPath("$[0].nomFoyer").value("Foyer A"))
                .andExpect(jsonPath("$[1].idFoyer").value(2))
                .andExpect(jsonPath("$[1].nomFoyer").value("Foyer B"));

        verify(foyerService, times(1)).retrieveAllFoyers();
    }

    @Test
    void testRetrieveFoyer() throws Exception {
        // Arrange
        Long foyerId = 1L;
        when(foyerService.retrieveFoyer(foyerId)).thenReturn(foyer1);

        // Act & Assert
        mockMvc.perform(get("/foyer/retrieve-foyer/{foyer-id}", foyerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idFoyer").value(1))
                .andExpect(jsonPath("$.nomFoyer").value("Foyer A"))
                .andExpect(jsonPath("$.capaciteFoyer").value(100));

        verify(foyerService, times(1)).retrieveFoyer(foyerId);
    }

    @Test
    void testAddFoyer() throws Exception {
        // Arrange
        Foyer newFoyer = new Foyer();
        newFoyer.setNomFoyer("Nouveau Foyer");
        newFoyer.setCapaciteFoyer(200);

        when(foyerService.addFoyer(any(Foyer.class))).thenReturn(newFoyer);

        // Act & Assert
        mockMvc.perform(post("/foyer/add-foyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newFoyer)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nomFoyer").value("Nouveau Foyer"))
                .andExpect(jsonPath("$.capaciteFoyer").value(200));

        verify(foyerService, times(1)).addFoyer(any(Foyer.class));
    }

    @Test
    void testModifyFoyer() throws Exception {
        // Arrange
        Foyer foyerToModify = new Foyer();
        foyerToModify.setIdFoyer(1L);
        foyerToModify.setNomFoyer("Foyer Modifié");
        foyerToModify.setCapaciteFoyer(300);

        when(foyerService.modifyFoyer(any(Foyer.class))).thenReturn(foyerToModify);

        // Act & Assert
        mockMvc.perform(put("/foyer/modify-foyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(foyerToModify)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idFoyer").value(1))
                .andExpect(jsonPath("$.nomFoyer").value("Foyer Modifié"))
                .andExpect(jsonPath("$.capaciteFoyer").value(300));

        verify(foyerService, times(1)).modifyFoyer(any(Foyer.class));
    }

    @Test
    void testRemoveFoyer() throws Exception {
        // Arrange
        Long foyerId = 1L;
        doNothing().when(foyerService).removeFoyer(foyerId);

        // Act & Assert
        mockMvc.perform(delete("/foyer/remove-foyer/{foyer-id}", foyerId))
                .andExpect(status().isOk());

        verify(foyerService, times(1)).removeFoyer(foyerId);
    }

    @Test
    void testAddFoyer_WithInvalidData() throws Exception {
        // Arrange
        Foyer invalidFoyer = new Foyer();
        // Foyer sans nom (données invalides)

        // Act & Assert
        mockMvc.perform(post("/foyer/add-foyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidFoyer)))
                .andExpect(status().isOk()); // Le contrôleur ne fait pas de validation

        verify(foyerService, times(1)).addFoyer(any(Foyer.class));
    }
} 