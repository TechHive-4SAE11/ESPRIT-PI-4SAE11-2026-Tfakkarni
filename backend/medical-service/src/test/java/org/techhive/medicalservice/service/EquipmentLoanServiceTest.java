package org.techhive.medicalservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.medicalservice.dto.EquipmentLoanDTO;
import org.techhive.medicalservice.entity.Equipment;
import org.techhive.medicalservice.entity.EquipmentLoan;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;
import org.techhive.medicalservice.entity.enums.LoanStatus;
import org.techhive.medicalservice.repository.EquipmentLoanRepository;
import org.techhive.medicalservice.repository.EquipmentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentLoanServiceTest {

    @Mock
    private EquipmentLoanRepository loanRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private IEquipmentService equipmentService;

    @InjectMocks
    private IEquipmentLoanServiceImp loanService;

    private Equipment sampleEquipment;
    private EquipmentLoan sampleLoan;
    private EquipmentLoanDTO sampleLoanDTO;

    @BeforeEach
    void setUp() {
        sampleEquipment = new Equipment();
        sampleEquipment.setId(1L);
        sampleEquipment.setName("Wheelchair");
        sampleEquipment.setStatus(EquipmentStatus.AVAILABLE);

        sampleLoan = new EquipmentLoan();
        sampleLoan.setId(1L);
        sampleLoan.setEquipment(sampleEquipment);
        sampleLoan.setBorrowerId(50L);
        sampleLoan.setLoanDate(LocalDateTime.now());
        sampleLoan.setDueDate(LocalDateTime.now().plusDays(14));
        sampleLoan.setStatus(LoanStatus.ACTIVE);

        sampleLoanDTO = EquipmentLoanDTO.builder()
                .id(1L)
                .equipmentId(1L)
                .borrowerId(50L)
                .loanDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(14))
                .status(LoanStatus.ACTIVE)
                .build();
    }

    @Test
    void createLoan_withValidData_shouldReturnSavedLoan() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(sampleEquipment));
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(sampleEquipment);
        when(loanRepository.save(any(EquipmentLoan.class))).thenReturn(sampleLoan);

        EquipmentLoan result = loanService.createLoan(sampleLoanDTO);

        assertNotNull(result);
        assertEquals(50L, result.getBorrowerId());
        assertEquals(LoanStatus.ACTIVE, result.getStatus());
        verify(equipmentRepository).save(any(Equipment.class)); // equipment status updated
    }

    @Test
    void createLoan_withNonExistentEquipment_shouldReturnNull() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        EquipmentLoanDTO dto = EquipmentLoanDTO.builder()
                .equipmentId(99L)
                .borrowerId(50L)
                .build();

        EquipmentLoan result = loanService.createLoan(dto);

        assertNull(result);
    }

    @Test
    void getLoanById_whenExists_shouldReturnLoan() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(sampleLoan));

        EquipmentLoan result = loanService.getLoanById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void returnEquipment_withActiveLoan_shouldUpdateStatusToReturned() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(sampleLoan));
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(sampleEquipment);
        when(loanRepository.save(any(EquipmentLoan.class))).thenReturn(sampleLoan);

        EquipmentLoan result = loanService.returnEquipment(1L);

        assertNotNull(result);
        assertEquals(LoanStatus.RETURNED, result.getStatus());
        assertNotNull(result.getReturnDate());
        assertEquals(EquipmentStatus.AVAILABLE, sampleEquipment.getStatus());
    }

    @Test
    void returnEquipment_withNonActiveLoan_shouldReturnNull() {
        sampleLoan.setStatus(LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(sampleLoan));

        EquipmentLoan result = loanService.returnEquipment(1L);

        assertNull(result);
    }

    @Test
    void returnEquipment_whenLoanNotFound_shouldReturnNull() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        EquipmentLoan result = loanService.returnEquipment(99L);

        assertNull(result);
    }

    @Test
    void extendLoan_withActiveLoan_shouldExtendDueDate() {
        LocalDateTime originalDueDate = sampleLoan.getDueDate();
        when(loanRepository.findById(1L)).thenReturn(Optional.of(sampleLoan));
        when(loanRepository.save(any(EquipmentLoan.class))).thenReturn(sampleLoan);

        EquipmentLoan result = loanService.extendLoan(1L, 7);

        assertNotNull(result);
        assertTrue(result.getDueDate().isAfter(originalDueDate));
    }

    @Test
    void extendLoan_withInactiveLoan_shouldReturnNull() {
        sampleLoan.setStatus(LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(sampleLoan));

        EquipmentLoan result = loanService.extendLoan(1L, 7);

        assertNull(result);
    }

    @Test
    void cancelLoan_shouldSetStatusToCancelled() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(sampleLoan));
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(sampleEquipment);
        when(loanRepository.save(any(EquipmentLoan.class))).thenReturn(sampleLoan);

        EquipmentLoan result = loanService.cancelLoan(1L);

        assertNotNull(result);
        assertEquals(LoanStatus.CANCELLED, result.getStatus());
        assertEquals(EquipmentStatus.AVAILABLE, sampleEquipment.getStatus());
    }

    @Test
    void borrowEquipment_whenAvailable_shouldCreateLoan() {
        when(equipmentService.isEquipmentAvailable(1L)).thenReturn(true);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(sampleEquipment));
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(sampleEquipment);
        when(loanRepository.save(any(EquipmentLoan.class))).thenReturn(sampleLoan);

        EquipmentLoan result = loanService.borrowEquipment(sampleLoanDTO);

        assertNotNull(result);
    }

    @Test
    void borrowEquipment_whenNotAvailable_shouldReturnNull() {
        when(equipmentService.isEquipmentAvailable(1L)).thenReturn(false);

        EquipmentLoan result = loanService.borrowEquipment(sampleLoanDTO);

        assertNull(result);
    }

    @Test
    void getLoansByBorrowerId_shouldReturnList() {
        when(loanRepository.findByBorrowerId(50L)).thenReturn(List.of(sampleLoan));

        List<EquipmentLoan> result = loanService.getLoansByBorrowerId(50L);

        assertEquals(1, result.size());
    }

    @Test
    void countActiveLoansByBorrower_shouldReturnCount() {
        when(loanRepository.countByBorrowerIdAndStatus(50L, LoanStatus.ACTIVE)).thenReturn(3L);

        long count = loanService.countActiveLoansByBorrower(50L);

        assertEquals(3L, count);
    }

    @Test
    void isEquipmentLoaned_shouldDelegateToRepository() {
        when(loanRepository.isEquipmentLoaned(1L)).thenReturn(true);

        boolean loaned = loanService.isEquipmentLoaned(1L);

        assertTrue(loaned);
    }

    @Test
    void checkAndUpdateOverdueLoans_shouldAutoReturnExpiredLoans() {
        when(loanRepository.findOverdueLoans()).thenReturn(List.of(sampleLoan));
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(sampleEquipment);
        when(loanRepository.save(any(EquipmentLoan.class))).thenReturn(sampleLoan);

        loanService.checkAndUpdateOverdueLoans();

        assertEquals(LoanStatus.RETURNED, sampleLoan.getStatus());
        assertEquals(EquipmentStatus.AVAILABLE, sampleEquipment.getStatus());
        verify(loanRepository).save(sampleLoan);
    }
}
