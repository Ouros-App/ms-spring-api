package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.category.CategoryRequestDTO;
import com.ourosapp.springapi.dto.category.CategoryResponseDTO;
import com.ourosapp.springapi.entity.Category;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.Tip;
import com.ourosapp.springapi.entity.TipCategory;
import com.ourosapp.springapi.repository.CategoryRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.TipCategoryRepository;
import com.ourosapp.springapi.repository.TipRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pelas regras de negócio e persistência de {@link Category}.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TipRepository tipRepository;
    private final TipCategoryRepository tipCategoryRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;

    /**
     * Cadastra uma nova categoria para classificação de dicas técnicas.
     *
     * @param request   dados da categoria
     * @param principal dados do usuário logado
     * @return DTO com os dados da categoria cadastrada
     */
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (!ADM.equals(role) && !COMPANY_EMPLOYEE.equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para cadastrar categorias de dicas"
            );
        }

        Tip tip = tipRepository.findById(request.idTip())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dica técnica não encontrada para o ID: " + request.idTip()
                ));

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            Farm farm = findFarmByIdOrThrow(tip.getIdFarm());
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Funcionário não tem permissão para vincular categoria a uma dica de outra empresa"
                );
            }
        }

        Category category = Category.builder()
                .category(request.category())
                .build();

        Category saved = categoryRepository.save(category);

        if (!tipCategoryRepository.existsByIdTipAndIdCategory(tip.getId(), saved.getId())) {
            tipCategoryRepository.save(TipCategory.builder()
                    .idTip(tip.getId())
                    .idCategory(saved.getId())
                    .build());
        }

        return CategoryResponseDTO.fromEntity(saved);
    }

    /**
     * Retorna a lista de todas as categorias cadastradas no sistema.
     *
     * @param principal dados do usuário logado
     * @return lista de DTOs de categorias
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getCategories(UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (!ADM.equals(role) && !COMPANY_EMPLOYEE.equals(role) && !FARM_OWNER.equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para listar categorias"
            );
        }

        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponseDTO::fromEntity)
                .toList();
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    private CompanyEmployee getCompanyEmployeeOrThrow(Long id) {
        return companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário logado não encontrado para o ID: " + id
                ));
    }

    private Farm findFarmByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada para o ID: null");
        }
        return farmRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fazenda não encontrada para o ID: " + id
                ));
    }
}
