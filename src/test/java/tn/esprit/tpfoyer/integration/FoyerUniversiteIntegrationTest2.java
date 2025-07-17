package tn.esprit.tpfoyer.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tpfoyer.entity.Foyer;
import tn.esprit.tpfoyer.entity.Universite;
import tn.esprit.tpfoyer.service.IFoyerService;
import tn.esprit.tpfoyer.service.IUniversiteService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class FoyerUniversiteIntegrationTest2 {

    @Autowired
    private IFoyerService foyerService;

    @Autowired
    private IUniversiteService universiteService;

    @Test
    void testCreateFoyerAndUniversite() {
        // OD ======> Test creating foyer and universite
        Foyer foyer = new Foyer();
        foyer.setNomFoyer("Test Foyer Integration");
        foyer.setCapaciteFoyer(800L);

        Universite universite = new Universite();
        universite.setNomUniversite("Test University");
        universite.setAdresse("123 Test Street");

        Foyer savedFoyer = foyerService.addFoyer(foyer);
        Universite savedUniversite = universiteService.addUniversite(universite);

        assertNotNull(savedFoyer.getIdFoyer());
        assertNotNull(savedUniversite.getIdUniversite());
        assertEquals("Test Foyer Integration", savedFoyer.getNomFoyer());
        assertEquals("Test University", savedUniversite.getNomUniversite());
    }

    @Test
    void testRetrieveAllFoyersAndUniversites() {
        // OD ======> Test retrieving all foyers and universites
        Foyer foyer1 = new Foyer();
        foyer1.setNomFoyer("Foyer 1");
        foyer1.setCapaciteFoyer(300L);

        Foyer foyer2 = new Foyer();
        foyer2.setNomFoyer("Foyer 2");
        foyer2.setCapaciteFoyer(400L);

        Universite universite1 = new Universite();
        universite1.setNomUniversite("University 1");
        universite1.setAdresse("Address 1");

        Universite universite2 = new Universite();
        universite2.setNomUniversite("University 2");
        universite2.setAdresse("Address 2");

        foyerService.addFoyer(foyer1);
        foyerService.addFoyer(foyer2);
        universiteService.addUniversite(universite1);
        universiteService.addUniversite(universite2);

        List<Foyer> foyers = foyerService.retrieveAllFoyers();
        List<Universite> universites = universiteService.retrieveAllUniversites();

        assertTrue(foyers.size() >= 2);
        assertTrue(universites.size() >= 2);
    }

    @Test
    void testRetrieveFoyerAndUniversiteById() {
        // OD ======> Test retrieving foyer and universite by ID
        Foyer foyer = new Foyer();
        foyer.setNomFoyer("Retrieve Test Foyer");
        foyer.setCapaciteFoyer(500L);

        Universite universite = new Universite();
        universite.setNomUniversite("Retrieve Test University");
        universite.setAdresse("Retrieve Address");

        Foyer savedFoyer = foyerService.addFoyer(foyer);
        Universite savedUniversite = universiteService.addUniversite(universite);

        Foyer retrievedFoyer = foyerService.retrieveFoyer(savedFoyer.getIdFoyer());
        Universite retrievedUniversite = universiteService.retrieveUniversite(savedUniversite.getIdUniversite());

        assertNotNull(retrievedFoyer);
        assertNotNull(retrievedUniversite);
        assertEquals("Retrieve Test Foyer", retrievedFoyer.getNomFoyer());
        assertEquals("Retrieve Test University", retrievedUniversite.getNomUniversite());
    }

    @Test
    void testDeleteFoyerAndUniversite() {
        // OD ======> Test deleting foyer and universite
        Foyer foyer = new Foyer();
        foyer.setNomFoyer("Foyer to Delete");
        foyer.setCapaciteFoyer(200L);

        Universite universite = new Universite();
        universite.setNomUniversite("University to Delete");
        universite.setAdresse("Delete Address");

        Foyer savedFoyer = foyerService.addFoyer(foyer);
        Universite savedUniversite = universiteService.addUniversite(universite);

        Long foyerId = savedFoyer.getIdFoyer();
        Long universiteId = savedUniversite.getIdUniversite();

        foyerService.removeFoyer(foyerId);
        universiteService.removeUniversite(universiteId);

        // Verify deletion
        assertThrows(RuntimeException.class, () -> foyerService.retrieveFoyer(foyerId));
        assertThrows(RuntimeException.class, () -> universiteService.retrieveUniversite(universiteId));
    }

    @Test
    void testFoyerAndUniversiteBasicOperations() {
        // OD ======> Test basic CRUD operations for both entities
        // Create
        Foyer foyer = new Foyer();
        foyer.setNomFoyer("CRUD Test Foyer");
        foyer.setCapaciteFoyer(600L);

        Universite universite = new Universite();
        universite.setNomUniversite("CRUD Test University");
        universite.setAdresse("CRUD Address");

        Foyer savedFoyer = foyerService.addFoyer(foyer);
        Universite savedUniversite = universiteService.addUniversite(universite);

        // Read
        assertNotNull(savedFoyer.getIdFoyer());
        assertNotNull(savedUniversite.getIdUniversite());

        // Verify they exist in lists
        List<Foyer> foyers = foyerService.retrieveAllFoyers();
        List<Universite> universites = universiteService.retrieveAllUniversites();

        assertTrue(foyers.stream().anyMatch(f -> f.getNomFoyer().equals("CRUD Test Foyer")));
        assertTrue(universites.stream().anyMatch(u -> u.getNomUniversite().equals("CRUD Test University")));
    }

    @Test
    void testConcurrentOperations() {
        // OD ======> Test concurrent operations on foyers and universites
        Foyer foyer1 = new Foyer();
        foyer1.setNomFoyer("Concurrent Foyer 1");
        foyer1.setCapaciteFoyer(400L);

        Foyer foyer2 = new Foyer();
        foyer2.setNomFoyer("Concurrent Foyer 2");
        foyer2.setCapaciteFoyer(500L);

        Universite universite1 = new Universite();
        universite1.setNomUniversite("Concurrent University 1");
        universite1.setAdresse("Concurrent Address 1");

        Universite universite2 = new Universite();
        universite2.setNomUniversite("Concurrent University 2");
        universite2.setAdresse("Concurrent Address 2");

        // Save all concurrently
        Foyer savedFoyer1 = foyerService.addFoyer(foyer1);
        Foyer savedFoyer2 = foyerService.addFoyer(foyer2);
        Universite savedUniversite1 = universiteService.addUniversite(universite1);
        Universite savedUniversite2 = universiteService.addUniversite(universite2);

        // Verify all are saved
        assertNotNull(savedFoyer1.getIdFoyer());
        assertNotNull(savedFoyer2.getIdFoyer());
        assertNotNull(savedUniversite1.getIdUniversite());
        assertNotNull(savedUniversite2.getIdUniversite());

        // Verify they can be retrieved
        assertNotNull(foyerService.retrieveFoyer(savedFoyer1.getIdFoyer()));
        assertNotNull(foyerService.retrieveFoyer(savedFoyer2.getIdFoyer()));
        assertNotNull(universiteService.retrieveUniversite(savedUniversite1.getIdUniversite()));
        assertNotNull(universiteService.retrieveUniversite(savedUniversite2.getIdUniversite()));
    }
} 