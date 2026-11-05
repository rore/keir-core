package dev.keir.examples;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GenericCapacityTest {
    static void capacity(Runnable action) {
        assertEquals("CAPACITY_EXCEEDED", assertThrows(IllegalArgumentException.class, action::run).getMessage());
    }

    @Test void finiteModelAndProgramReservationsRejectBeforeCallbacksAndReleaseExactlyOnce() {
        var budget=new GenericCapacity(); var models=new ArrayList<GenericCapacity.Lease>(); int[] callbacks={0};
        for(int i=0;i<4;i++) { models.add(budget.model(65_536,65_536,65_536)); callbacks[0]++; }
        capacity(()->{ try(var ignored=budget.model(1,1,1)) { callbacks[0]++; } }); assertEquals(4,callbacks[0]);
        models.getFirst().close(); models.getFirst().close(); try(var reload=budget.model(65_536,65_536,65_536)) { assertNotNull(reload); }
        models.forEach(GenericCapacity.Lease::close);
        for(int field=0;field<3;field++) { final int current=field; capacity(()->budget.model(current==0?65_537:0,current==1?65_537:0,current==2?65_537:0)); }
        var programs=new ArrayList<GenericCapacity.Lease>(); for(int i=0;i<8;i++)programs.add(budget.program(1_048_576));
        capacity(()->budget.program(1)); capacity(()->new GenericCapacity().program(1_048_577)); programs.forEach(GenericCapacity.Lease::close);
        try(var reload=budget.program(1_048_576)) { assertNotNull(reload); }
    }
    @Test void bindPrepareAndInvocationConcurrencyHaveNoQueue() {
        var budget=new GenericCapacity(); int[] callbacks={0};
        try(var binding=budget.work()) { capacity(()->{ try(var preparation=budget.work()) { callbacks[0]++; } }); }
        try(var preparation=budget.work()) { capacity(()->{ try(var binding=budget.work()) { callbacks[0]++; } }); }
        try(var first=budget.invocation();var second=budget.invocation()) { capacity(()->{ try(var third=budget.invocation()) { callbacks[0]++; } }); }
        assertEquals(0,callbacks[0]); try(var fresh=budget.work();var invocation=budget.invocation()) { assertNotNull(fresh); assertNotNull(invocation); }
    }
    @Test void everyScenarioCapacityHasBoundaryAndPlusOne() {
        GenericCapacity.scenarios(1_048_576,128,65_536,4,4);
        for(int field=0;field<5;field++) { int current=field; capacity(()->GenericCapacity.scenarios(current==0?1_048_577:0,current==1?129:0,current==2?65_537:0,current==3?5:0,current==4?5:0)); }
    }
}
