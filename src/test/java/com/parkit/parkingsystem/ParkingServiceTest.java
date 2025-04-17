package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;


    @BeforeEach
    private void setUpPerTest() {
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("AB-123-CD");
            
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("AB-123-CD");
            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }
        

    @Test
    public void processExitingVehicleTest() {
        when(ticketDAO.getNbTicket("AB-123-CD")).thenReturn(1); // Utilisateur récurrent

        parkingService.processExitingVehicle();
        
        ArgumentCaptor<Ticket> cap = ArgumentCaptor.forClass(Ticket.class); //initialisons d'un ArgumentCaptor typé sur la classe Ticket.
        verify(ticketDAO, times(1)).updateTicket(cap.capture()); //Verif que ticketDAO.updateTicket a été appelée une seule fois. À ce moment-là, l’ArgumentCaptor va capturer l’instance de Ticket transmise lors de cet appel.
        Ticket updatedTicket = cap.getValue(); //on extrait l’objet Ticket capturé par l’ArgumentCaptor grâce à la méthode getValue()
        
        ArgumentCaptor<ParkingSpot> parkingSpotCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        verify(parkingSpotDAO, times(1)).updateParking(parkingSpotCaptor.capture()); //Verif que la méthode pour mettre à jour la place de parking (updateParking) a bien été appelée une seule fois.
        ParkingSpot updatedSpot = parkingSpotCaptor.getValue();
       
        assertTrue(updatedSpot.isAvailable(), "La place de parking doit être libérée lors de la sortie"); // Vérifie que la place est de nouveau disponible
        assertNotNull(updatedTicket.getOutTime(), "Pas d'heure de sortie renséignée"); //Verifier qu'une heure de sortie sur le ticket est maintenant renseignée
        
        // Prix attendu pour 1h de parking, -5% 
        assertEquals(1.5 * 0.95, updatedTicket.getPrice(), 0.01, "La réduction doit s'appliquer");
    }
    

    
}
