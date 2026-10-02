package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.internal.PasswordResetInternalDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serviço responsável por operações administrativas internas de autenticação e credenciais.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalAuthService {

    private final FarmOwnerRepository farmOwnerRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Atualiza a senha de um usuário (produtor rural ou funcionário) no banco de dados.
     * Codifica a nova senha com o algoritmo BCrypt configurado.
     *
     * @param dto dados da redefinição contendo tipo de conta, identificador e nova senha em texto plano
     * @throws ResponseStatusException 404 se a entidade não for encontrada, ou 400 se o tipo de conta for inválido
     */
    @Transactional
    public void resetPassword(PasswordResetInternalDTO dto) {
        String encodedPassword = passwordEncoder.encode(dto.newPassword());

        if ("farm_owner".equalsIgnoreCase(dto.accountType())) {
            FarmOwner farmOwner = farmOwnerRepository.findById(dto.id())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Produtor rural não encontrado para o ID informado: " + dto.id()
                    ));
            farmOwner.setPassword(encodedPassword);
            farmOwnerRepository.save(farmOwner);
            log.info("Senha do produtor rural ID {} redefinida com sucesso via rota interna", dto.id());
        } else if ("company_employee".equalsIgnoreCase(dto.accountType())) {
            CompanyEmployee employee = companyEmployeeRepository.findById(dto.id())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Funcionário não encontrado para o ID informado: " + dto.id()
                    ));
            employee.setPassword(encodedPassword);
            companyEmployeeRepository.save(employee);
            log.info("Senha do funcionário ID {} redefinida com sucesso via rota interna", dto.id());
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo de conta inválido para redefinição de senha: " + dto.accountType()
            );
        }
    }
}
