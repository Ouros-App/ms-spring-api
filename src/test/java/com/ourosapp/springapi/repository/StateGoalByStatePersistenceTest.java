package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.dto.stategoal.*;
import com.ourosapp.springapi.entity.*;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.StateGoalByStateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(StateGoalByStateService.class)
class StateGoalByStatePersistenceTest {
    @Autowired StateGoalByStateService service;
    @Autowired AddressRepository addresses;
    @Autowired FarmRepository farms;
    @Autowired EnterpriseRepository enterprises;
    @Autowired CompanyEmployeeRepository employees;
    @Autowired StateGoalRepository goals;
    @Autowired FarmGoalRepository links;
    @Autowired PlatformTransactionManager transactionManager;

    private Address address(String state, String country) {
        return addresses.saveAndFlush(Address.builder().zipCode("01001000").state(state).country(country)
                .city("Cidade").number("1").build());
    }
    private Enterprise enterprise(String name) {
        return enterprises.saveAndFlush(Enterprise.builder().name(name).email(name+"@example.com")
                .documentNumber(String.format("%014d", Math.abs((long) name.hashCode()))).telephone("11999999999").idAddress(address("SP","BR").getId()).build());
    }
    private Farm farm(String state,String country,Long enterpriseId) {
        return farms.saveAndFlush(Farm.builder().name("Granja").areaProperty(BigDecimal.ONE).region("Sudeste")
                .idAddress(address(state,country).getId()).idEnterprise(enterpriseId).build());
    }
    private StateGoalByStateRequestDTO request(Long enterpriseId) {
        return new StateGoalByStateRequestDTO("sp",enterpriseId,new StateGoalTaskRequestDTO("Realizar manutenção",null,LocalDate.of(2026,1,1),LocalDate.of(2026,2,1)));
    }
    private UserPrincipal admin() { return new UserPrincipal(1L,"adm@example.com",null,"ADM",List.of()); }

    @Test void deveSelecionarPeloEstadoDoEnderecoEPersistirUmaMetaParaVariasGranjas() {
        Long own = enterprise("empresa1").getId();
        Long other = enterprise("empresa2").getId();
        farm("SP","BR",own); farm("SP","BR",own);
        farm("RJ","BR",own); farm("SP","BR",other); farm("SP","US",own);
        assertEquals(2,service.preview("SP",own,admin()).participantsCount());
        farm("SP","BR",own); // A criação recalcula o público, sem confiar na prévia.
        var result = service.create(request(own),admin());
        assertEquals(3,result.participantsCount());
        assertEquals(1,goals.count());
        var saved = goals.findById(result.id()).orElseThrow();
        assertEquals("TASK_COMPLETION",saved.getType());
        assertEquals(0,BigDecimal.ONE.compareTo(saved.getTargetValue()));
        var participants = links.findByIdGoal(result.id());
        assertEquals(3,participants.size());
        assertTrue(participants.stream().allMatch(link -> link.getStatus().equals("Pendente")));
        participants.get(0).setStatus("Concluída"); links.saveAndFlush(participants.get(0));
        assertEquals(2,links.findByIdGoal(result.id()).stream().filter(link -> link.getStatus().equals("Pendente")).count());
    }

    @Test void deveReverterMetaEVinculosQuandoATransacaoFalha() {
        long beforeGoals = goals.count(); long beforeLinks = links.count();
        var tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThrows(IllegalStateException.class,() -> tx.execute(status -> {
            Long id = enterprise("rollback").getId(); farm("SP","BR",id); farm("SP","BR",id);
            service.create(request(id),admin());
            goals.flush(); links.flush();
            throw new IllegalStateException("Falha simulada antes do commit");
        }));
        assertEquals(beforeGoals,goals.count()); assertEquals(beforeLinks,links.count());
    }
}
