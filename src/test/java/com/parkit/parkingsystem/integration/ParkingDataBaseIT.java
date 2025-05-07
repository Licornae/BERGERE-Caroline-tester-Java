package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    
    private static final String VEH_REG = "AB-123-CD";

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEH_REG);
        dataBasePrepareService.clearDataBaseEntries();
        
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        
        Ticket ticket = ticketDAO.getTicket(VEH_REG); 						
        
        assertNotNull(ticket, "Le ticket doit être enregistré en BDD");             

        assertNotNull(ticket.getInTime(), "L'heure d'entrée doit être renseignée"); 
        
        ParkingSpot Spot = ticket.getParkingSpot();                                
        ParkingSpot parking = parkingSpotDAO.getParkingSpot(Spot.getId());        
        boolean dispo = parking.isAvailable();                                    
        assertFalse(dispo, "La place ne doit plus être disponible");
        assertEquals(parking.getParkingType(), ParkingType.CAR);                    
      
    }

    @Test
    public void testParkingLotExit() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket(VEH_REG);
        assertNotNull(ticket, "Le ticket doit exister après l'entrée !");
        java.util.Date inTime = ticket.getInTime();

        Date outTime = new Date(inTime.getTime() + 60 * 60 * 1000);
        ticket.setOutTime(outTime);

        new FareCalculatorService().calculateFare(ticket, false);

        boolean updated = ticketDAO.updateTicket(ticket);
        assertTrue(updated, "Le ticket doit être mis à jour correctement");

        Ticket updatedTicket = ticketDAO.getTicketWithOutTime(VEH_REG);
        assertNotNull(updatedTicket, "Le ticket doit exister après la sortie !");
        assertNotNull(updatedTicket.getOutTime(), "L'heure de sortie doit être renseignée !");

        double expectedPrice = 1.5;
        assertEquals(expectedPrice, updatedTicket.getPrice(), 0.01, "Le prix pour 1h de voiture doit être correct");
    }
    
    @Test
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        Ticket oldTicket = new Ticket();
        oldTicket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        oldTicket.setVehicleRegNumber(VEH_REG);
        oldTicket.setInTime(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)); 
        oldTicket.setOutTime(new Date(System.currentTimeMillis() - 23 * 60 * 60 * 1000));
        oldTicket.setPrice(1.5);
        ticketDAO.saveTicket(oldTicket);

        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket(VEH_REG);
        assertNotNull(ticket);

        Date simulatedInTime = new Date(System.currentTimeMillis() - 60 * 60 * 1000);
        ticket.setInTime(simulatedInTime);

        try (Connection con = dataBaseTestConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE ticket SET IN_TIME = ? WHERE ID = ?");
            ps.setTimestamp(1, new Timestamp(simulatedInTime.getTime()));
            ps.setInt(2, ticket.getId());
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            fail("Erreur lors de la mise à jour de inTime dans la BDD : " + e.getMessage());
        }

        parkingService.processExitingVehicle();

        Ticket updatedTicket = ticketDAO.getTicketWithOutTime(VEH_REG);
        assertNotNull(updatedTicket.getOutTime());

        double expectedPrice = 1.5 * 0.95; 
        assertEquals(expectedPrice, updatedTicket.getPrice(), 0.01, "Le prix pour un utilisateur récurrent doit inclure la remise de 5%");
    }

}
