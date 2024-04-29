package de.prob2.ui.simulation.diagram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.inject.Injector;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

public class DiagramGeneratorTest {
    DiagramGenerator gen; 

    @BeforeEach
    public void before(){
        StageManager sm = Mockito.mock(StageManager.class);
        FileChooserManager fcm = Mockito.mock(FileChooserManager.class);
        Injector inj = Mockito.mock(Injector.class);
        I18n i18n = Mockito.mock(I18n.class);
        RealTimeSimulator rts = Mockito.mock(RealTimeSimulator.class);


        
        gen = new DiagramGenerator(sm, fcm, null, null, inj, i18n, rts);

    }
    
    @Test
    @DisplayName("Test that checks that nodes get correctly extracted")
    public void test1(){
        gen.generateDiagram();
        
        
    }
    
}
