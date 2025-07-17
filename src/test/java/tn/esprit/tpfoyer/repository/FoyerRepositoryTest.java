package tn.esprit.tpfoyer.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import tn.esprit.tpfoyer.entity.Foyer;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class FoyerRepositoryTest {

    @Autowired
    private FoyerRepository foyerRepository;

    @Test
    void testSaveAndFindById() {
        // OD ======> Test saving foyer and finding by ID
        Foyer foyer = new Foyer();
        foyer.setNomFoyer("Test Foyer");
        foyer.setCapaciteFoyer(500L);

        Foyer savedFoyer = foyerRepository.save(foyer);
        
        assertNotNull(savedFoyer);
        assertNotNull(savedFoyer.getIdFoyer());
        assertEquals("Test Foyer", savedFoyer.getNomFoyer());
        assertEquals(500L, savedFoyer.getCapaciteFoyer());

        Optional<Foyer> foundFoyer = foyerRepository.findById(savedFoyer.getIdFoyer());
        assertTrue(foundFoyer.isPresent());
        assertEquals("Test Foyer", foundFoyer.get().getNomFoyer());
    }

    @Test
    void testFindAll() {
        // OD ======> Test finding all foyers
        Foyer foyer1 = new Foyer();
        foyer1.setNomFoyer("Foyer 1");
        foyer1.setCapaciteFoyer(300L);

        Foyer foyer2 = new Foyer();
        foyer2.setNomFoyer("Foyer 2");
        foyer2.setCapaciteFoyer(400L);

        foyerRepository.save(foyer1);
        foyerRepository.save(foyer2);

        List<Foyer> foyers = foyerRepository.findAll();
        assertTrue(foyers.size() >= 2);
    }

    @Test
    void testDeleteById() {
        // OD ======> Test deleting foyer by ID
        Foyer foyer = new Foyer();
        foyer.setNomFoyer("Test Foyer");
        foyer.setCapaciteFoyer(500L);

        Foyer savedFoyer = foyerRepository.save(foyer);
        Long foyerId = savedFoyer.getIdFoyer();

        foyerRepository.deleteById(foyerId);

        Optional<Foyer> foundFoyer = foyerRepository.findById(foyerId);
        assertFalse(foundFoyer.isPresent());
    }

    @Test
    void testUpdateFoyer() {
        // OD ======> Test updating foyer
        Foyer foyer = new Foyer();
        foyer.setNomFoyer("Original Name");
        foyer.setCapaciteFoyer(300L);

        Foyer savedFoyer = foyerRepository.save(foyer);
        
        savedFoyer.setNomFoyer("Updated Name");
        savedFoyer.setCapaciteFoyer(400L);
        
        Foyer updatedFoyer = foyerRepository.save(savedFoyer);
        
        assertEquals("Updated Name", updatedFoyer.getNomFoyer());
        assertEquals(400L, updatedFoyer.getCapaciteFoyer());
    }

    @Test
    void testCountFoyers() {
        // OD ======> Test counting foyers
        long initialCount = foyerRepository.count();
        
        Foyer foyer = new Foyer();
        foyer.setNomFoyer("Count Test Foyer");
        foyer.setCapaciteFoyer(200L);

        foyerRepository.save(foyer);

        long newCount = foyerRepository.count();
        assertEquals(initialCount + 1, newCount);
    }

    @Test
    void testExistsById() {
        // OD ======> Test checking if foyer exists by ID
        Foyer foyer = new Foyer();
        foyer.setNomFoyer("Exists Test Foyer");
        foyer.setCapaciteFoyer(350L);

        Foyer savedFoyer = foyerRepository.save(foyer);
        Long foyerId = savedFoyer.getIdFoyer();

        assertTrue(foyerRepository.existsById(foyerId));
        assertFalse(foyerRepository.existsById(99999L));
    }
} 