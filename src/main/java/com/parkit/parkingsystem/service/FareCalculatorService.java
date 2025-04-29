package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {
	
    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
    
    public void calculateFare(Ticket ticket, boolean discount){ 
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inMillis = ticket.getInTime().getTime();
        long outMillis = ticket.getOutTime().getTime();

        long durationMillis = outMillis - inMillis;
        
        double durationHours = durationMillis / (1000.0 * 60 * 60);
        
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
        
		if (discount) {
			ticket.setPrice(ticket.getPrice() * 0.95);
		}
    }
}