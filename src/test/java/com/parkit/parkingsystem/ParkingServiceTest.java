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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private  ParkingService parkingService;

    @Mock
    private  InputReaderUtil inputReaderUtil;
    @Mock
    private ParkingSpotDAO parkingSpotDAO;
    @Mock
    private  TicketDAO ticketDAO;

    private static final String VEH_REG = "AB-123-CD";
    private ParkingSpot parkingSpot;
    private Ticket ticket;
    
    @BeforeEach
    void setUpPerTest() {
        try {
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
            
            parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
            
            ticket = new Ticket();
            
            ticket.setInTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber(VEH_REG);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }
        
    @Test
    public void testProcessIncomingVehicle() throws Exception { 	
    	
		when(inputReaderUtil.readSelection()).thenReturn(1); 								
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEH_REG);
    	when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1); 			
    	
    	parkingService.processIncomingVehicle();
    	
        ArgumentCaptor<ParkingSpot> spotCaptor = ArgumentCaptor.forClass(ParkingSpot.class); 
        verify(parkingSpotDAO).updateParking(spotCaptor.capture());							
        ParkingSpot spotUsed = spotCaptor.getValue();										 
        
        assertEquals(1, spotUsed.getId());													 
        assertFalse(spotUsed.isAvailable());												 
        assertEquals(ParkingType.CAR, spotUsed.getParkingType());						     

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);         
        verify(ticketDAO).saveTicket(ticketCaptor.capture());							     
        Ticket savedTicket = ticketCaptor.getValue();                                        
        
        assertEquals("AB-123-CD", savedTicket.getVehicleRegNumber());                        
        assertEquals(spotUsed, savedTicket.getParkingSpot());								 
        assertNotNull(savedTicket.getInTime());											     
        assertNull(savedTicket.getOutTime());                                                
    }
    
    
    @Test
    public void processExitingVehicleTest() throws Exception {              
        
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEH_REG); 							
		when(ticketDAO.getTicket(VEH_REG)).thenReturn(ticket); 							    				
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);                   				
        when(ticketDAO.getNbTicket(VEH_REG)).thenReturn(1); 												
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);        				

        parkingService.processExitingVehicle();
        
        ArgumentCaptor<Ticket> cap = ArgumentCaptor.forClass(Ticket.class);                 				
        verify(ticketDAO, times(1)).updateTicket(cap.capture());                            				
        Ticket updatedTicket = cap.getValue();                                              				
        
        assertNotNull(updatedTicket.getOutTime(), "Pas d'heure de sortie renseignée");      				
        
        ArgumentCaptor<ParkingSpot> parkingSpotCaptor = ArgumentCaptor.forClass(ParkingSpot.class); 		
        verify(parkingSpotDAO, times(1)).updateParking(parkingSpotCaptor.capture());         				
        ParkingSpot updatedSpot = parkingSpotCaptor.getValue();									    		
       
        assertTrue(updatedSpot.isAvailable(), "La place de parking doit être libérée lors de la sortie"); 	
        assertEquals(1.5 * 0.95, updatedTicket.getPrice(), 0.01, "La réduction doit s'appliquer");          
    }
    
    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {                         
    	
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEH_REG);
        when(ticketDAO.getTicket(VEH_REG)).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);                        
        
    	parkingService.processExitingVehicle();
    	
        verify(ticketDAO).getTicket(VEH_REG);													   
        verify(ticketDAO).updateTicket(any(Ticket.class)); 										  
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));                
    } 
    
    @Test
    public void testGetNextParkingNumberIfAvailable() {                                           
        
    	ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,true);                      
    	
        assertNotNull(parkingSpot);                                                              
        assertEquals(1, parkingSpot.getId());                                                  
        assertTrue(parkingSpot.isAvailable());                                                    
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {                      
    	
		when(inputReaderUtil.readSelection()).thenReturn(1); 									  
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0);                      

        ParkingSpot noParkingSpot = parkingService.getNextParkingNumberIfAvailable();            

        assertNull(noParkingSpot);                                                               
    } 

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {                 

        when(inputReaderUtil.readSelection()).thenReturn(3);                                      

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();               

        assertNull(parkingSpot);                                                                  
    } 

    @Test
    public void testProcessIncomingVehicleRecurringUser() throws Exception {

    	ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    	PrintStream originalOut = System.out;
    	System.setOut(new PrintStream(outContent));

    	when(inputReaderUtil.readSelection()).thenReturn(1);
    	when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEH_REG);
    	when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
    	when(ticketDAO.getNbTicket(VEH_REG)).thenReturn(1);

    	parkingService.processIncomingVehicle();

    	System.setOut(originalOut);

    	String output = outContent.toString();
    	assertTrue(output.contains("Heureux de vous revoir ! En tant qu’utilisateur régulier de notre parking, vous allez obtenir une remise de 5%"),
    			"Le message pour utilisateur récurrent devrait être présent.");
    }
    
    @Test
    public void testGetNextParkingNumberIfAvailable_Bike() {
    	
    	when(inputReaderUtil.readSelection()).thenReturn(2); 
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE)).thenReturn(5);
        
        ParkingSpot spot = parkingService.getNextParkingNumberIfAvailable();
        
        assertNotNull(spot);
        assertEquals(ParkingType.BIKE, spot.getParkingType());
    }

    @Test
    public void testProcessExitingVehicleCatchBlock() throws Exception {
    	
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEH_REG);
        
        when(ticketDAO.getTicket(VEH_REG)).thenThrow(new RuntimeException("DB Error"));
        
        parkingService.processExitingVehicle(); 
    }
    
    @Test
    public void testProcessIncomingVehicleCatchBlock() throws Exception {
    	
        ParkingService parkingServiceSpy = Mockito.spy(parkingService);
        
        doThrow(new RuntimeException("Boom!")).when(parkingServiceSpy).getNextParkingNumberIfAvailable();

        parkingServiceSpy.processIncomingVehicle();
    }
    
}