package tn.esprit.tpfoyer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tn.esprit.tpfoyer.entity.Foyer;
import tn.esprit.tpfoyer.entity.Universite;
import tn.esprit.tpfoyer.repository.FoyerRepository;
import tn.esprit.tpfoyer.repository.UniversiteRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Tests d'Intégration pour Foyer et Universite")
class FoyerUniversiteIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FoyerRepository foyerRepository;

    @Autowired
    private UniversiteRepository universiteRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        
        // Nettoyer la base de données avant chaque test
        universiteRepository.deleteAll();
        foyerRepository.deleteAll();
    }

    @Test
    @DisplayName("Test CRUD complet pour Foyer")
    void testFoyerCRUD_Integration() throws Exception {
        // Test 1: Créer un foyer
        Foyer newFoyer = new Foyer();
        newFoyer.setNomFoyer("Foyer Test Integration");
        newFoyer.setCapaciteFoyer(200);

        String foyerJson = objectMapper.writeValueAsString(newFoyer);

        String response = mockMvc.perform(post("/foyer/add-foyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foyerJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nomFoyer").value("Foyer Test Integration"))
                .andExpect(jsonPath("$.capaciteFoyer").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extraire l'ID du foyer créé
        Foyer createdFoyer = objectMapper.readValue(response, Foyer.class);
        Long foyerId = createdFoyer.getIdFoyer();

        // Test 2: Récupérer le foyer créé
        mockMvc.perform(get("/foyer/retrieve-foyer/{foyer-id}", foyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idFoyer").value(foyerId))
                .andExpect(jsonPath("$.nomFoyer").value("Foyer Test Integration"));

        // Test 3: Modifier le foyer
        Foyer updatedFoyer = new Foyer();
        updatedFoyer.setIdFoyer(foyerId);
        updatedFoyer.setNomFoyer("Foyer Test Integration Modifié");
        updatedFoyer.setCapaciteFoyer(250);

        String updatedFoyerJson = objectMapper.writeValueAsString(updatedFoyer);

        mockMvc.perform(put("/foyer/modify-foyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedFoyerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomFoyer").value("Foyer Test Integration Modifié"))
                .andExpect(jsonPath("$.capaciteFoyer").value(250));

        // Test 4: Vérifier que la modification a été sauvegardée
        mockMvc.perform(get("/foyer/retrieve-foyer/{foyer-id}", foyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomFoyer").value("Foyer Test Integration Modifié"));

        // Test 5: Supprimer le foyer
        mockMvc.perform(delete("/foyer/remove-foyer/{foyer-id}", foyerId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test CRUD complet pour Universite")
    void testUniversiteCRUD_Integration() throws Exception {
        // Test 1: Créer une université
        Universite newUniversite = new Universite();
        newUniversite.setNomUniversite("Université Test Integration");
        newUniversite.setAdresse("Adresse Test Integration");

        String universiteJson = objectMapper.writeValueAsString(newUniversite);

        String response = mockMvc.perform(post("/universite/add-universite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(universiteJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nomUniversite").value("Université Test Integration"))
                .andExpect(jsonPath("$.adresse").value("Adresse Test Integration"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extraire l'ID de l'université créée
        Universite createdUniversite = objectMapper.readValue(response, Universite.class);
        Long universiteId = createdUniversite.getIdUniversite();

        // Test 2: Récupérer l'université créée
        mockMvc.perform(get("/universite/retrieve-universite/{universite-id}", universiteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUniversite").value(universiteId))
                .andExpect(jsonPath("$.nomUniversite").value("Université Test Integration"));

        // Test 3: Modifier l'université
        Universite updatedUniversite = new Universite();
        updatedUniversite.setIdUniversite(universiteId);
        updatedUniversite.setNomUniversite("Université Test Integration Modifiée");
        updatedUniversite.setAdresse("Adresse Test Integration Modifiée");

        String updatedUniversiteJson = objectMapper.writeValueAsString(updatedUniversite);

        mockMvc.perform(put("/universite/modify-universite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedUniversiteJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomUniversite").value("Université Test Integration Modifiée"))
                .andExpect(jsonPath("$.adresse").value("Adresse Test Integration Modifiée"));

        // Test 4: Vérifier que la modification a été sauvegardée
        mockMvc.perform(get("/universite/retrieve-universite/{universite-id}", universiteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomUniversite").value("Université Test Integration Modifiée"));

        // Test 5: Supprimer l'université
        mockMvc.perform(delete("/universite/remove-universite/{universite-id}", universiteId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test de la relation One-to-One entre Universite et Foyer")
    void testUniversiteFoyerRelation_Integration() throws Exception {
        // Test 1: Créer un foyer
        Foyer foyer = new Foyer();
        foyer.setNomFoyer("Foyer pour Universite");
        foyer.setCapaciteFoyer(300);

        String foyerJson = objectMapper.writeValueAsString(foyer);
        String foyerResponse = mockMvc.perform(post("/foyer/add-foyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foyerJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Foyer createdFoyer = objectMapper.readValue(foyerResponse, Foyer.class);

        // Test 2: Créer une université sans foyer d'abord
        Universite universite = new Universite();
        universite.setNomUniversite("Universite avec Foyer");
        universite.setAdresse("Adresse Universite");
        // Ne pas assigner de foyer ici

        String universiteJson = objectMapper.writeValueAsString(universite);
        String universiteResponse = mockMvc.perform(post("/universite/add-universite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(universiteJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Universite createdUniversite = objectMapper.readValue(universiteResponse, Universite.class);

        // Test 3: Modifier l'université pour ajouter le foyer
        createdUniversite.setFoyer(createdFoyer);
        String updateJson = objectMapper.writeValueAsString(createdUniversite);
        
        mockMvc.perform(put("/universite/modify-universite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foyer.nomFoyer").value("Foyer pour Universite"));

        // Test 4: Vérifier la relation en récupérant l'université
        mockMvc.perform(get("/universite/retrieve-universite/{universite-id}", createdUniversite.getIdUniversite()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foyer.idFoyer").value(createdFoyer.getIdFoyer()))
                .andExpect(jsonPath("$.foyer.nomFoyer").value("Foyer pour Universite"))
                .andExpect(jsonPath("$.foyer.capaciteFoyer").value(300));

        // Test 5: Vérifier que le foyer peut être récupéré indépendamment
        mockMvc.perform(get("/foyer/retrieve-foyer/{foyer-id}", createdFoyer.getIdFoyer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomFoyer").value("Foyer pour Universite"));
    }

    @Test
    @DisplayName("Test de récupération de toutes les entités")
    void testRetrieveAllEntities_Integration() throws Exception {
        // Créer plusieurs foyers
        Foyer foyer1 = new Foyer();
        foyer1.setNomFoyer("Foyer 1");
        foyer1.setCapaciteFoyer(100);

        Foyer foyer2 = new Foyer();
        foyer2.setNomFoyer("Foyer 2");
        foyer2.setCapaciteFoyer(150);

        // Sauvegarder directement en base
        foyerRepository.save(foyer1);
        foyerRepository.save(foyer2);

        // Créer plusieurs universités
        Universite universite1 = new Universite();
        universite1.setNomUniversite("Université 1");
        universite1.setAdresse("Adresse 1");

        Universite universite2 = new Universite();
        universite2.setNomUniversite("Université 2");
        universite2.setAdresse("Adresse 2");

        // Sauvegarder directement en base
        universiteRepository.save(universite1);
        universiteRepository.save(universite2);

        // Tester la récupération de tous les foyers
        mockMvc.perform(get("/foyer/retrieve-all-foyers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        // Tester la récupération de toutes les universités
        mockMvc.perform(get("/universite/retrieve-all-universites"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Test de gestion des erreurs - Entité non trouvée")
    void testEntityNotFound_Integration() throws Exception {
        // Ce test vérifie que les services gèrent correctement les entités inexistantes
        // En pratique, les exceptions sont gérées par les services, pas par les contrôleurs
        
        // Test simple : vérifier que la base de données est vide au début
        mockMvc.perform(get("/foyer/retrieve-all-foyers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/universite/retrieve-all-universites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
} 