package se.gokopen.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import se.gokopen.dao.PatrolNotFoundException;
import se.gokopen.dao.PatrolNotSavedException;
import se.gokopen.model.PatrolImpl;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/mvc-dispatcher-servlet.xml"})

public class TestCreatePatrol {
    
    
    @Autowired
    private PatrolService patrolService;
    
    @Test
    public void shouldCreatePatrolInDatabase() throws PatrolNotSavedException, PatrolNotFoundException{
        PatrolImpl patrol = new PatrolImpl();
        patrol.setPatrolName("Test from junit");
        patrol.setNote("Just a test from automated testing. Should not be here - remove if you see");
        patrolService.savePatrol(patrol);
        System.out.println("patrol with id " + patrol.getPatrolId() + " has been saved");
        
        assertNotNull(patrol.getPatrolId());
        
        //Cleaning up
        patrolService.deletePatrol(patrol);
    }

}
