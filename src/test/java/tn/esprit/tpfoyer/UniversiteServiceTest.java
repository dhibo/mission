package tn.esprit.tpfoyer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.tpfoyer.entity.Foyer;
import tn.esprit.tpfoyer.entity.Universite;
import tn.esprit.tpfoyer.repository.UniversiteRepository;
import tn.esprit.tpfoyer.service.UniversiteServiceImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitaires pour UniversiteService")
class UniversiteServiceTest {

    @Mock
    private UniversiteRepository universiteRepository;

    @InjectMocks
    private UniversiteServiceImpl universiteService;

    private Universite universite1;
    private Universite universite2;
    private Foyer foyer;

    @BeforeEach
    void setUp() {
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
        universite2.setFoyer(null); // Pas de foyer
    }

    @Test
    @DisplayName("Devrait récupérer toutes les universités")
    void testRetrieveAllUniversites() {
        // ARRANGE
        List<Universite> expectedUniversites = Arrays.asList(universite1, universite2);
        when(universiteRepository.findAll()).thenReturn(expectedUniversites);

        // ACT
        List<Universite> actualUniversites = universiteService.retrieveAllUniversites();

        // ASSERT
        assertNotNull(actualUniversites);
        assertEquals(2, actualUniversites.size());
        assertEquals("Université de Tunis", actualUniversites.get(0).getNomUniversite());
        assertEquals("Université de Sfax", actualUniversites.get(1).getNomUniversite());
        verify(universiteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Devrait récupérer une université par ID quand elle existe")
    void testRetrieveUniversite_WhenUniversiteExists() {
        // ARRANGE
        Long universiteId = 1L;
        when(universiteRepository.findById(universiteId)).thenReturn(Optional.of(universite1));

        // ACT
        Universite actualUniversite = universiteService.retrieveUniversite(universiteId);

        // ASSERT
        assertNotNull(actualUniversite);
        assertEquals(universiteId, actualUniversite.getIdUniversite());
        assertEquals("Université de Tunis", actualUniversite.getNomUniversite());
        assertEquals("Tunis, Tunisie", actualUniversite.getAdresse());
        assertNotNull(actualUniversite.getFoyer());
        assertEquals("Foyer Principal", actualUniversite.getFoyer().getNomFoyer());
        verify(universiteRepository, times(1)).findById(universiteId);
    }

    @Test
    @DisplayName("Devrait lever une exception quand l'université n'existe pas")
    void testRetrieveUniversite_WhenUniversiteDoesNotExist() {
        // ARRANGE
        Long universiteId = 999L;
        when(universiteRepository.findById(universiteId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(java.util.NoSuchElementException.class, () -> {
            universiteService.retrieveUniversite(universiteId);
        });
        verify(universiteRepository, times(1)).findById(universiteId);
    }

    @Test
    @DisplayName("Devrait ajouter une nouvelle université")
    void testAddUniversite() {
        // ARRANGE
        Universite newUniversite = new Universite();
        newUniversite.setNomUniversite("Nouvelle Université");
        newUniversite.setAdresse("Nouvelle Adresse");
        newUniversite.setFoyer(foyer);

        when(universiteRepository.save(any(Universite.class))).thenReturn(newUniversite);

        // ACT
        Universite savedUniversite = universiteService.addUniversite(newUniversite);

        // ASSERT
        assertNotNull(savedUniversite);
        assertEquals("Nouvelle Université", savedUniversite.getNomUniversite());
        assertEquals("Nouvelle Adresse", savedUniversite.getAdresse());
        assertNotNull(savedUniversite.getFoyer());
        verify(universiteRepository, times(1)).save(newUniversite);
    }

    @Test
    @DisplayName("Devrait ajouter une université sans foyer")
    void testAddUniversite_WithoutFoyer() {
        // ARRANGE
        Universite newUniversite = new Universite();
        newUniversite.setNomUniversite("Université Sans Foyer");
        newUniversite.setAdresse("Adresse Test");
        // Pas de foyer assigné

        when(universiteRepository.save(any(Universite.class))).thenReturn(newUniversite);

        // ACT
        Universite savedUniversite = universiteService.addUniversite(newUniversite);

        // ASSERT
        assertNotNull(savedUniversite);
        assertEquals("Université Sans Foyer", savedUniversite.getNomUniversite());
        assertNull(savedUniversite.getFoyer());
        verify(universiteRepository, times(1)).save(newUniversite);
    }

    @Test
    @DisplayName("Devrait modifier une université existante")
    void testModifyUniversite() {
        // ARRANGE
        Universite universiteToModify = new Universite();
        universiteToModify.setIdUniversite(1L);
        universiteToModify.setNomUniversite("Université Modifiée");
        universiteToModify.setAdresse("Adresse Modifiée");
        universiteToModify.setFoyer(foyer);

        when(universiteRepository.save(any(Universite.class))).thenReturn(universiteToModify);

        // ACT
        Universite modifiedUniversite = universiteService.modifyUniversite(universiteToModify);

        // ASSERT
        assertNotNull(modifiedUniversite);
        assertEquals("Université Modifiée", modifiedUniversite.getNomUniversite());
        assertEquals("Adresse Modifiée", modifiedUniversite.getAdresse());
        assertNotNull(modifiedUniversite.getFoyer());
        verify(universiteRepository, times(1)).save(universiteToModify);
    }

    @Test
    @DisplayName("Devrait supprimer une université")
    void testRemoveUniversite() {
        // ARRANGE
        Long universiteId = 1L;
        doNothing().when(universiteRepository).deleteById(universiteId);

        // ACT
        universiteService.removeUniversite(universiteId);

        // ASSERT
        verify(universiteRepository, times(1)).deleteById(universiteId);
    }

    @Test
    @DisplayName("Devrait gérer une liste vide d'universités")
    void testRetrieveAllUniversites_WhenEmpty() {
        // ARRANGE
        when(universiteRepository.findAll()).thenReturn(Arrays.asList());

        // ACT
        List<Universite> actualUniversites = universiteService.retrieveAllUniversites();

        // ASSERT
        assertNotNull(actualUniversites);
        assertTrue(actualUniversites.isEmpty());
        verify(universiteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Devrait gérer une université avec des données nulles")
    void testAddUniversite_WithNullData() {
        // ARRANGE
        Universite universiteWithNullData = new Universite();
        // Pas de données définies (toutes nulles)

        when(universiteRepository.save(any(Universite.class))).thenReturn(universiteWithNullData);

        // ACT
        Universite savedUniversite = universiteService.addUniversite(universiteWithNullData);

        // ASSERT
        assertNotNull(savedUniversite);
        verify(universiteRepository, times(1)).save(universiteWithNullData);
    }
} 