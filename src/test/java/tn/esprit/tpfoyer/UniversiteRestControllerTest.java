package tn.esprit.tpfoyer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.tpfoyer.control.UniversiteRestController;
import tn.esprit.tpfoyer.entity.Foyer;
import tn.esprit.tpfoyer.entity.Universite;
import tn.esprit.tpfoyer.service.IUniversiteService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitaires pour UniversiteRestController")
class UniversiteRestControllerTest {

    @Mock
    private IUniversiteService universiteService;

    @InjectMocks
    private UniversiteRestController universiteRestController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Universite universite1;
    private Universite universite2;
    private Foyer foyer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(universiteRestController).build();
        objectMapper = new ObjectMapper();

        // Créer un foyer pour les tests
        foyer = new Foyer();
        foyer.setIdFoyer(1L);
        foyer.setNomFoyer("Foyer Principal");
        foyer.setCapaciteFoyer(500);

        // Initialisation des universités de test
        universite1 = new Universite();
        universite1.setIdUniversite(1L);
        universite1.setNomUniversite("Université de Tunis");
        universite1.setAdresse("Tunis, Tunisie");
        universite1.setFoyer(foyer);

        universite2 = new Universite();
        universite2.setIdUniversite(2L);
        universite2.setNomUniversite("Université de Sfax");
        universite2.setAdresse("Sfax, Tunisie");
        universite2.setFoyer(null);
    }

    @Test
    @DisplayName("GET /universite/retrieve-all-universites - Devrait retourner toutes les universités")
    void testGetAllUniversites() throws Exception {
        // ARRANGE
        List<Universite> universites = Arrays.asList(universite1, universite2);
        when(universiteService.retrieveAllUniversites()).thenReturn(universites);

        // ACT & ASSERT
        mockMvc.perform(get("/universite/retrieve-all-universites"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].idUniversite").value(1))
                .andExpect(jsonPath("$[0].nomUniversite").value("Université de Tunis"))
                .andExpect(jsonPath("$[0].adresse").value("Tunis, Tunisie"))
                .andExpect(jsonPath("$[1].idUniversite").value(2))
                .andExpect(jsonPath("$[1].nomUniversite").value("Université de Sfax"))
                .andExpect(jsonPath("$[1].adresse").value("Sfax, Tunisie"));

        verify(universiteService, times(1)).retrieveAllUniversites();
    }

    @Test
    @DisplayName("GET /universite/retrieve-universite/{id} - Devrait retourner une université spécifique")
    void testGetUniversiteById() throws Exception {
        // ARRANGE
        Long universiteId = 1L;
        when(universiteService.retrieveUniversite(universiteId)).thenReturn(universite1);

        // ACT & ASSERT
        mockMvc.perform(get("/universite/retrieve-universite/{universite-id}", universiteId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idUniversite").value(1))
                .andExpect(jsonPath("$.nomUniversite").value("Université de Tunis"))
                .andExpect(jsonPath("$.adresse").value("Tunis, Tunisie"))
                .andExpect(jsonPath("$.foyer.nomFoyer").value("Foyer Principal"));

        verify(universiteService, times(1)).retrieveUniversite(universiteId);
    }

    @Test
    @DisplayName("POST /universite/add-universite - Devrait créer une nouvelle université")
    void testAddUniversite() throws Exception {
        // ARRANGE
        Universite newUniversite = new Universite();
        newUniversite.setNomUniversite("Nouvelle Université");
        newUniversite.setAdresse("Nouvelle Adresse");
        newUniversite.setFoyer(foyer);

        when(universiteService.addUniversite(any(Universite.class))).thenReturn(newUniversite);

        // ACT & ASSERT
        mockMvc.perform(post("/universite/add-universite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUniversite)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nomUniversite").value("Nouvelle Université"))
                .andExpect(jsonPath("$.adresse").value("Nouvelle Adresse"))
                .andExpect(jsonPath("$.foyer.nomFoyer").value("Foyer Principal"));

        verify(universiteService, times(1)).addUniversite(any(Universite.class));
    }

    @Test
    @DisplayName("POST /universite/add-universite - Devrait créer une université sans foyer")
    void testAddUniversite_WithoutFoyer() throws Exception {
        // ARRANGE
        Universite newUniversite = new Universite();
        newUniversite.setNomUniversite("Université Sans Foyer");
        newUniversite.setAdresse("Adresse Test");
        // Pas de foyer

        when(universiteService.addUniversite(any(Universite.class))).thenReturn(newUniversite);

        // ACT & ASSERT
        mockMvc.perform(post("/universite/add-universite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUniversite)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomUniversite").value("Université Sans Foyer"))
                .andExpect(jsonPath("$.adresse").value("Adresse Test"));

        verify(universiteService, times(1)).addUniversite(any(Universite.class));
    }

    @Test
    @DisplayName("PUT /universite/modify-universite - Devrait modifier une université existante")
    void testModifyUniversite() throws Exception {
        // ARRANGE
        Universite universiteToModify = new Universite();
        universiteToModify.setIdUniversite(1L);
        universiteToModify.setNomUniversite("Université Modifiée");
        universiteToModify.setAdresse("Adresse Modifiée");
        universiteToModify.setFoyer(foyer);

        when(universiteService.modifyUniversite(any(Universite.class))).thenReturn(universiteToModify);

        // ACT & ASSERT
        mockMvc.perform(put("/universite/modify-universite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(universiteToModify)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idUniversite").value(1))
                .andExpect(jsonPath("$.nomUniversite").value("Université Modifiée"))
                .andExpect(jsonPath("$.adresse").value("Adresse Modifiée"));

        verify(universiteService, times(1)).modifyUniversite(any(Universite.class));
    }

    @Test
    @DisplayName("DELETE /universite/remove-universite/{id} - Devrait supprimer une université")
    void testRemoveUniversite() throws Exception {
        // ARRANGE
        Long universiteId = 1L;
        doNothing().when(universiteService).removeUniversite(universiteId);

        // ACT & ASSERT
        mockMvc.perform(delete("/universite/remove-universite/{universite-id}", universiteId))
                .andExpect(status().isOk());

        verify(universiteService, times(1)).removeUniversite(universiteId);
    }

    @Test
    @DisplayName("GET /universite/retrieve-all-universites - Devrait gérer une liste vide")
    void testGetAllUniversites_WhenEmpty() throws Exception {
        // ARRANGE
        when(universiteService.retrieveAllUniversites()).thenReturn(Arrays.asList());

        // ACT & ASSERT
        mockMvc.perform(get("/universite/retrieve-all-universites"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(universiteService, times(1)).retrieveAllUniversites();
    }

    @Test
    @DisplayName("POST /universite/add-universite - Devrait gérer des données invalides")
    void testAddUniversite_WithInvalidData() throws Exception {
        // ARRANGE
        Universite invalidUniversite = new Universite();
        // Pas de données définies

        when(universiteService.addUniversite(any(Universite.class))).thenReturn(invalidUniversite);

        // ACT & ASSERT
        mockMvc.perform(post("/universite/add-universite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUniversite)))
                .andExpect(status().isOk()); // Le contrôleur ne fait pas de validation

        verify(universiteService, times(1)).addUniversite(any(Universite.class));
    }
} 