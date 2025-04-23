package com.parkit.parkingsystem.integration.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;

import org.junit.jupiter.api.Test;

public class TestDBConnection {
	
	private DataBaseTestConfig dataBaseConfig = new DataBaseTestConfig();
	
	@Test
	 void testGetConnection() {
        // Teste si la connexion peut être établie sans exception
        Connection connection = null;
     try {
        connection = dataBaseConfig.getConnection();
        assertNotNull(connection, "La connexion à la base de données n'a pas pu être établie !");
        System.out.println("Connexion à la base de données réussie !");
    } catch (Exception e) {
        fail("Une exception a été levée lors de la tentative de connexion : " + e.getMessage());
    } finally {
        // Ferme la connexion une fois le test terminé
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
}