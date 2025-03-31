package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {
	
    // Méthode pour le calcul standard (sans réduction)
    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
    
    public void calculateFare(Ticket ticket, boolean discount){ 
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inMillis = ticket.getInTime().getTime();
        long outMillis = ticket.getOutTime().getTime();

        //Durée en milliseconds
        long durationMillis = outMillis - inMillis;
        
        // Calcul de la durée en heures (millisecondes converties en heures)
        double durationHours = durationMillis / (1000.0 * 60 * 60);
        
        //Si la durée est inférieure à 30 minutes, on considère que la durée est de 0 heure pour que le ticket soit gratuit
		if (durationHours < 0.5) {   
			durationHours = 0;
		}
        
        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(durationHours * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                ticket.setPrice(durationHours * Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
        
        //Si le client a une réduction, on applique une réduction de 5% sur le prix total
		if (discount) {
			ticket.setPrice(ticket.getPrice() * 0.95);
		}
    }
}