package tn.esprit.tpfoyer.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import tn.esprit.tpfoyer.entity.Etudiant;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class EtudiantRepositoryTest {

    @Autowired
    private EtudiantRepository etudiantRepository;

    @Test
    void testSaveAndFindById() {
        // OD ======> Test saving etudiant and finding by ID
        Etudiant etudiant = new Etudiant();
        etudiant.setNomEtudiant("Dupont");
        etudiant.setPrenomEtudiant("Jean");
        etudiant.setCinEtudiant(12345678L);
        etudiant.setDateNaissance(new Date());

        Etudiant savedEtudiant = etudiantRepository.save(etudiant);
        
        assertNotNull(savedEtudiant);
        assertNotNull(savedEtudiant.getIdEtudiant());
        assertEquals("Dupont", savedEtudiant.getNomEtudiant());
        assertEquals("Jean", savedEtudiant.getPrenomEtudiant());
        assertEquals(12345678L, savedEtudiant.getCinEtudiant());

        Optional<Etudiant> foundEtudiant = etudiantRepository.findById(savedEtudiant.getIdEtudiant());
        assertTrue(foundEtudiant.isPresent());
        assertEquals("Dupont", foundEtudiant.get().getNomEtudiant());
    }

    @Test
    void testFindAll() {
        // OD ======> Test finding all etudiants
        Etudiant etudiant1 = new Etudiant();
        etudiant1.setNomEtudiant("Martin");
        etudiant1.setPrenomEtudiant("Marie");
        etudiant1.setCinEtudiant(87654321L);
        etudiant1.setDateNaissance(new Date());

        Etudiant etudiant2 = new Etudiant();
        etudiant2.setNomEtudiant("Durand");
        etudiant2.setPrenomEtudiant("Pierre");
        etudiant2.setCinEtudiant(11111111L);
        etudiant2.setDateNaissance(new Date());

        etudiantRepository.save(etudiant1);
        etudiantRepository.save(etudiant2);

        List<Etudiant> etudiants = etudiantRepository.findAll();
        assertTrue(etudiants.size() >= 2);
    }

    @Test
    void testDeleteById() {
        // OD ======> Test deleting etudiant by ID
        Etudiant etudiant = new Etudiant();
        etudiant.setNomEtudiant("Test");
        etudiant.setPrenomEtudiant("Student");
        etudiant.setCinEtudiant(99999999L);
        etudiant.setDateNaissance(new Date());

        Etudiant savedEtudiant = etudiantRepository.save(etudiant);
        Long etudiantId = savedEtudiant.getIdEtudiant();

        etudiantRepository.deleteById(etudiantId);

        Optional<Etudiant> foundEtudiant = etudiantRepository.findById(etudiantId);
        assertFalse(foundEtudiant.isPresent());
    }

    @Test
    void testUpdateEtudiant() {
        // OD ======> Test updating etudiant
        Etudiant etudiant = new Etudiant();
        etudiant.setNomEtudiant("Original");
        etudiant.setPrenomEtudiant("Name");
        etudiant.setCinEtudiant(33333333L);
        etudiant.setDateNaissance(new Date());

        Etudiant savedEtudiant = etudiantRepository.save(etudiant);
        
        savedEtudiant.setNomEtudiant("Updated");
        savedEtudiant.setPrenomEtudiant("Name");
        
        Etudiant updatedEtudiant = etudiantRepository.save(savedEtudiant);
        
        assertEquals("Updated", updatedEtudiant.getNomEtudiant());
        assertEquals("Name", updatedEtudiant.getPrenomEtudiant());
    }

    @Test
    void testCountEtudiants() {
        // OD ======> Test counting etudiants
        long initialCount = etudiantRepository.count();
        
        Etudiant etudiant = new Etudiant();
        etudiant.setNomEtudiant("Count");
        etudiant.setPrenomEtudiant("Test");
        etudiant.setCinEtudiant(44444444L);
        etudiant.setDateNaissance(new Date());

        etudiantRepository.save(etudiant);

        long newCount = etudiantRepository.count();
        assertEquals(initialCount + 1, newCount);
    }

    @Test
    void testExistsById() {
        // OD ======> Test checking if etudiant exists by ID
        Etudiant etudiant = new Etudiant();
        etudiant.setNomEtudiant("Exists");
        etudiant.setPrenomEtudiant("Test");
        etudiant.setCinEtudiant(55555555L);
        etudiant.setDateNaissance(new Date());

        Etudiant savedEtudiant = etudiantRepository.save(etudiant);
        Long etudiantId = savedEtudiant.getIdEtudiant();

        assertTrue(etudiantRepository.existsById(etudiantId));
        assertFalse(etudiantRepository.existsById(99999L));
    }
} 