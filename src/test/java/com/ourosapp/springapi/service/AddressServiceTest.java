package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.address.AddressRequestDTO;
import com.ourosapp.springapi.dto.address.AddressResponseDTO;
import com.ourosapp.springapi.dto.address.AddressUpdateDTO;
import com.ourosapp.springapi.entity.Address;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Enterprise;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.AddressRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @InjectMocks
    private AddressService addressService;

    private Address sampleAddress;
    private AddressRequestDTO sampleRequest;
    private UserPrincipal adminPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal farmOwnerPrincipal;

    @BeforeEach
    void setUp() {
        sampleAddress = Address.builder()
                .id(1L)
                .zipCode("01310-100")
                .state("SP")
                .city("São Paulo")
                .number("1000")
                .country("BR")
                .build();

        sampleRequest = new AddressRequestDTO(
                "01310-100",
                "SP",
                "São Paulo",
                "1000",
                "BR"
        );

        adminPrincipal = new UserPrincipal(
                1L,
                "admin@test.com",
                "secret",
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        employeePrincipal = new UserPrincipal(
                2L,
                "employee@test.com",
                "secret",
                "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        farmOwnerPrincipal = new UserPrincipal(
                3L,
                "farmer@test.com",
                "secret",
                "FARM_OWNER",
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
    }

    @Test
    @DisplayName("Deve cadastrar um novo endereço com sucesso")
    void testCreateAddressSuccess() {
        when(addressRepository.save(any(Address.class))).thenReturn(sampleAddress);

        AddressResponseDTO response = addressService.createAddress(sampleRequest);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("01310-100", response.zipCode());
        assertEquals("SP", response.state());
        assertEquals("São Paulo", response.city());
        assertEquals("1000", response.number());
        assertEquals("BR", response.country());

        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao tentar cadastrar endereço com payload nulo")
    void testCreateAddressNullPayloadThrowsException() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> addressService.createAddress(null)
        );

        assertEquals("O payload da requisição não pode ser nulo", exception.getMessage());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar endereço por ID com perfil ADM")
    void testGetAddressByIdAsAdmin() {
        when(addressRepository.findById(1L)).thenReturn(Optional.of(sampleAddress));

        AddressResponseDTO response = addressService.getAddressById(1L, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("01310-100", response.zipCode());
        verify(addressRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve retornar endereço por ID quando COMPANY_EMPLOYEE está vinculado à Empresa do endereço")
    void testGetAddressByIdAsCompanyEmployeeViaEnterprise() {
        CompanyEmployee employee = CompanyEmployee.builder()
                .id(2L)
                .idEnterprise(10L)
                .build();
        Enterprise enterprise = Enterprise.builder()
                .id(10L)
                .idAddress(1L)
                .build();

        when(addressRepository.findById(1L)).thenReturn(Optional.of(sampleAddress));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(enterpriseRepository.findAllByIdAddress(1L)).thenReturn(List.of(enterprise));

        AddressResponseDTO response = addressService.getAddressById(1L, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(enterpriseRepository, times(1)).findAllByIdAddress(1L);
    }

    @Test
    @DisplayName("Deve retornar endereço por ID quando COMPANY_EMPLOYEE está vinculado à Fazenda da mesma Empresa")
    void testGetAddressByIdAsCompanyEmployeeViaFarm() {
        CompanyEmployee employee = CompanyEmployee.builder()
                .id(2L)
                .idEnterprise(10L)
                .build();
        Farm farm = Farm.builder()
                .id(50L)
                .idEnterprise(10L)
                .idAddress(1L)
                .build();

        when(addressRepository.findById(1L)).thenReturn(Optional.of(sampleAddress));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(enterpriseRepository.findAllByIdAddress(1L)).thenReturn(List.of());
        when(farmRepository.findAllByIdAddress(1L)).thenReturn(List.of(farm));

        AddressResponseDTO response = addressService.getAddressById(1L, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(farmRepository, times(1)).findAllByIdAddress(1L);
    }

    @Test
    @DisplayName("Deve lançar 403 Forbidden quando COMPANY_EMPLOYEE não possui vínculo com o endereço")
    void testGetAddressByIdAsCompanyEmployeeForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder()
                .id(2L)
                .idEnterprise(10L)
                .build();

        when(addressRepository.findById(1L)).thenReturn(Optional.of(sampleAddress));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(enterpriseRepository.findAllByIdAddress(1L)).thenReturn(List.of());
        when(farmRepository.findAllByIdAddress(1L)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> addressService.getAddressById(1L, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Acesso negado a este endereço", ex.getReason());
    }

    @Test
    @DisplayName("Deve retornar endereço por ID quando FARM_OWNER está vinculado à Fazenda dona do endereço")
    void testGetAddressByIdAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder()
                .id(3L)
                .idFarm(50L)
                .build();
        Farm farm = Farm.builder()
                .id(50L)
                .idAddress(1L)
                .build();

        when(addressRepository.findById(1L)).thenReturn(Optional.of(sampleAddress));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findAllByIdAddress(1L)).thenReturn(List.of(farm));

        AddressResponseDTO response = addressService.getAddressById(1L, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve lançar 403 Forbidden quando FARM_OWNER não possui vínculo com o endereço")
    void testGetAddressByIdAsFarmOwnerForbidden() {
        FarmOwner owner = FarmOwner.builder()
                .id(3L)
                .idFarm(50L)
                .build();
        Farm otherFarm = Farm.builder()
                .id(99L)
                .idAddress(1L)
                .build();

        when(addressRepository.findById(1L)).thenReturn(Optional.of(sampleAddress));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findAllByIdAddress(1L)).thenReturn(List.of(otherFarm));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> addressService.getAddressById(1L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Acesso negado a este endereço", ex.getReason());
    }

    @Test
    @DisplayName("Deve lançar 401 Unauthorized quando principal for nulo na busca por ID")
    void testGetAddressByIdUnauthorized() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> addressService.getAddressById(1L, null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao buscar por ID inexistente")
    void testGetAddressByIdNotFound() {
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> addressService.getAddressById(99L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("99"));
        verify(addressRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Deve atualizar um endereço existente com sucesso como ADM")
    void testUpdateAddressSuccessAsAdmin() {
        AddressUpdateDTO updateRequest = new AddressUpdateDTO(
                "13010-001",
                "SP",
                "Campinas",
                "555",
                "BR"
        );

        Address updatedAddress = Address.builder()
                .id(1L)
                .zipCode("13010-001")
                .state("SP")
                .city("Campinas")
                .number("555")
                .country("BR")
                .build();

        when(addressRepository.findById(1L)).thenReturn(Optional.of(sampleAddress));
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);

        AddressResponseDTO response = addressService.updateAddress(1L, updateRequest, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("13010-001", response.zipCode());
        assertEquals("SP", response.state());
        assertEquals("Campinas", response.city());
        assertEquals("555", response.number());
        assertEquals("BR", response.country());

        verify(addressRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).save(sampleAddress);
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao tentar atualizar endereço inexistente")
    void testUpdateAddressNotFound() {
        AddressUpdateDTO updateRequest = new AddressUpdateDTO(
                "13010-001",
                "SP",
                "Campinas",
                "555",
                "BR"
        );

        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> addressService.updateAddress(99L, updateRequest, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("99"));
        verify(addressRepository, times(1)).findById(99L);
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 401 Unauthorized ao tentar atualizar sem autenticação")
    void testUpdateAddressUnauthorized() {
        AddressUpdateDTO updateRequest = new AddressUpdateDTO(
                "13010-001",
                "SP",
                "Campinas",
                "555",
                "BR"
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> addressService.updateAddress(1L, updateRequest, null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao tentar atualizar endereço com payload nulo")
    void testUpdateAddressNullPayloadThrowsException() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> addressService.updateAddress(1L, null, adminPrincipal)
        );

        assertEquals("O payload da requisição não pode ser nulo", exception.getMessage());
        verify(addressRepository, never()).findById(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar endereço sem chamar save quando payload de atualização não tiver campos informados")
    void testUpdateAddressWithoutUpdates() {
        AddressUpdateDTO emptyRequest = new AddressUpdateDTO(null, null, null, null, null);

        when(addressRepository.findById(1L)).thenReturn(Optional.of(sampleAddress));

        AddressResponseDTO response = addressService.updateAddress(1L, emptyRequest, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("01310-100", response.zipCode());
        verify(addressRepository, times(1)).findById(1L);
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar apenas campos informados individualmente")
    void testUpdateAddressPartialFields() {
        AddressUpdateDTO partialRequest = new AddressUpdateDTO(
                null,
                "RJ",
                null,
                "200",
                null
        );

        when(addressRepository.findById(1L)).thenReturn(Optional.of(sampleAddress));
        when(addressRepository.save(any(Address.class))).thenReturn(sampleAddress);

        AddressResponseDTO response = addressService.updateAddress(1L, partialRequest, adminPrincipal);

        assertNotNull(response);
        assertEquals("RJ", sampleAddress.getState());
        assertEquals("200", sampleAddress.getNumber());
        assertEquals("São Paulo", sampleAddress.getCity());
        verify(addressRepository, times(1)).save(sampleAddress);
    }
}
