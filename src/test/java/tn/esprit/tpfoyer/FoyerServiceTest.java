package tn.esprit.tpfoyer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.tpfoyer.entity.Foyer;
import tn.esprit.tpfoyer.repository.FoyerRepository;
import tn.esprit.tpfoyer.service.FoyerServiceImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoyerServiceTest {

    @Mock
    private FoyerRepository foyerRepository;

    @InjectMocks
    private FoyerServiceImpl foyerService;

    private Foyer foyer1;
    private Foyer foyer2;

    @BeforeEach
    void setUp() {
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
    void testRetrieveAllFoyers() {
        // Arrange
        List<Foyer> expectedFoyers = Arrays.asList(foyer1, foyer2);
        when(foyerRepository.findAll()).thenReturn(expectedFoyers);

        // Act
        List<Foyer> actualFoyers = foyerService.retrieveAllFoyers();

        // Assert
        assertNotNull(actualFoyers);
        assertEquals(2, actualFoyers.size());
        assertEquals("Foyer A", actualFoyers.get(0).getNomFoyer());
        assertEquals("Foyer B", actualFoyers.get(1).getNomFoyer());
        verify(foyerRepository, times(1)).findAll();
    }

    @Test
    void testRetrieveFoyer_WhenFoyerExists() {
        // Arrange
        Long foyerId = 1L;
        when(foyerRepository.findById(foyerId)).thenReturn(Optional.of(foyer1));

        // Act
        Foyer actualFoyer = foyerService.retrieveFoyer(foyerId);

        // Assert
        assertNotNull(actualFoyer);
        assertEquals(foyerId, actualFoyer.getIdFoyer());
        assertEquals("Foyer A", actualFoyer.getNomFoyer());
        verify(foyerRepository, times(1)).findById(foyerId);
    }

    @Test
    void testRetrieveFoyer_WhenFoyerDoesNotExist() {
        // Arrange
        Long foyerId = 999L;
        when(foyerRepository.findById(foyerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(java.util.NoSuchElementException.class, () -> {
            foyerService.retrieveFoyer(foyerId);
        });
        verify(foyerRepository, times(1)).findById(foyerId);
    }

    @Test
    void testAddFoyer() {
        // Arrange
        Foyer newFoyer = new Foyer();
        newFoyer.setNomFoyer("Nouveau Foyer");
        newFoyer.setCapaciteFoyer(200);

        when(foyerRepository.save(any(Foyer.class))).thenReturn(newFoyer);

        // Act
        Foyer savedFoyer = foyerService.addFoyer(newFoyer);

        // Assert
        assertNotNull(savedFoyer);
        assertEquals("Nouveau Foyer", savedFoyer.getNomFoyer());
        assertEquals(200, savedFoyer.getCapaciteFoyer());
        verify(foyerRepository, times(1)).save(newFoyer);
    }

    @Test
    void testModifyFoyer() {
        // Arrange
        Foyer foyerToModify = new Foyer();
        foyerToModify.setIdFoyer(1L);
        foyerToModify.setNomFoyer("Foyer Modifié");
        foyerToModify.setCapaciteFoyer(300);

        when(foyerRepository.save(any(Foyer.class))).thenReturn(foyerToModify);

        // Act
        Foyer modifiedFoyer = foyerService.modifyFoyer(foyerToModify);

        // Assert
        assertNotNull(modifiedFoyer);
        assertEquals("Foyer Modifié", modifiedFoyer.getNomFoyer());
        assertEquals(300, modifiedFoyer.getCapaciteFoyer());
        verify(foyerRepository, times(1)).save(foyerToModify);
    }

    @Test
    void testRemoveFoyer() {
        // Arrange
        Long foyerId = 1L;
        doNothing().when(foyerRepository).deleteById(foyerId);

        // Act
        foyerService.removeFoyer(foyerId);

        // Assert
        verify(foyerRepository, times(1)).deleteById(foyerId);
    }
}
