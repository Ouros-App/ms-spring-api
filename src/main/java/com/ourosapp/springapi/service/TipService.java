package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.tip.TipRequestDTO;
import com.ourosapp.springapi.dto.tip.TipResponseDTO;
import com.ourosapp.springapi.dto.tip.TipUpdateDTO;
import com.ourosapp.springapi.entity.*;
import com.ourosapp.springapi.repository.*;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelas regras de negócio e persistência de {@link Tip},
 * incluindo as tabelas de associação {@link TipCategory}, {@link FarmTip} e métricas de {@link Review}.
 */
@Service
@RequiredArgsConstructor
public class TipService {

    private final TipRepository tipRepository;
    private final CategoryRepository categoryRepository;
    private final TipCategoryRepository tipCategoryRepository;
    private final FarmTipRepository farmTipRepository;
    private final ReviewRepository reviewRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    /**
     * Cadastra uma nova dica técnica vinculada a uma fazenda e a suas categorias.
     *
     * @param request   dados da dica técnica
     * @param principal dados do usuário logado
     * @return DTO com os dados da dica cadastrada
     */
    @Transactional
    public TipResponseDTO createTip(TipRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (!ADM.equals(role) && !COMPANY_EMPLOYEE.equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para cadastrar dicas técnicas"
            );
        }

        Long farmId = resolveFarmIdForCreation(request.idFarm(), principal);
        Farm farm = findFarmByIdOrThrow(farmId);
        validateFarmAccessPermission(farm, principal, "cadastrar dicas técnicas nesta fazenda");

        List<Category> categories = validateAndFetchCategories(request.categoryIds());

        Tip tip = Tip.builder()
                .tip(request.tip())
                .idFarm(farm.getId())
                .build();

        Tip saved = tipRepository.save(tip);

        if (!farmTipRepository.existsByIdFarmAndIdTip(farm.getId(), saved.getId())) {
            farmTipRepository.save(FarmTip.builder()
                    .idFarm(farm.getId())
                    .idTip(saved.getId())
                    .build());
        }

        if (!categories.isEmpty()) {
            for (Category cat : categories) {
                if (!tipCategoryRepository.existsByIdTipAndIdCategory(saved.getId(), cat.getId())) {
                    tipCategoryRepository.save(TipCategory.builder()
                            .idTip(saved.getId())
                            .idCategory(cat.getId())
                            .build());
                }
            }
        }

        List<String> categoryNames = categories.stream()
                .map(Category::getCategory)
                .distinct()
                .toList();

        return TipResponseDTO.fromEntity(saved, categoryNames, 0, 0.0);
    }

    /**
     * Retorna a lista de dicas técnicas acessíveis ao usuário autenticado, com filtro opcional por fazenda.
     *
     * @param farmIdFilter identificador opcional da fazenda
     * @param principal    dados do usuário logado
     * @return lista de DTOs com as dicas técnicas
     */
    @Transactional(readOnly = true)
    public List<TipResponseDTO> getTipsForUser(Long farmIdFilter, UserPrincipal principal) {
        ensureAuthenticated(principal);

        if (farmIdFilter != null) {
            Farm farm = findFarmByIdOrThrow(farmIdFilter);
            validateFarmAccessPermission(farm, principal, "visualizar dicas técnicas desta fazenda");
            return getTipsForSingleFarm(farm);
        }

        String role = principal.getRole();
        if (ADM.equals(role)) {
            List<Tip> allTips = tipRepository.findAll();
            return enrichTips(allTips);
        } else if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            List<Farm> farms = farmRepository.findAllByIdEnterprise(employee.getIdEnterprise());
            if (farms.isEmpty()) {
                return List.of();
            }

            List<Long> farmIds = farms.stream().map(Farm::getId).toList();
            List<Tip> directTips = tipRepository.findByIdFarmIn(farmIds);
            List<FarmTip> linkedFarmTips = farmTipRepository.findByIdFarmIn(farmIds);
            List<Long> linkedTipIds = linkedFarmTips.stream().map(FarmTip::getIdTip).toList();
            List<Tip> junctionTips = linkedTipIds.isEmpty() ? List.of() : tipRepository.findAllById(linkedTipIds);

            Map<Long, Tip> combinedTips = new LinkedHashMap<>();
            directTips.forEach(t -> combinedTips.put(t.getId(), t));
            junctionTips.forEach(t -> combinedTips.putIfAbsent(t.getId(), t));

            return enrichTips(new ArrayList<>(combinedTips.values()));
        } else if (FARM_OWNER.equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (owner.getIdFarm() == null) {
                return List.of();
            }
            Farm farm = findFarmByIdOrThrow(owner.getIdFarm());
            return getTipsForSingleFarm(farm);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para listar dicas técnicas"
            );
        }
    }

    /**
     * Busca os detalhes de uma dica técnica específica pelo seu ID.
     *
     * @param id        identificador único da dica
     * @param principal dados do usuário logado
     * @return DTO com os detalhes da dica
     */
    @Transactional(readOnly = true)
    public TipResponseDTO getTipById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        Tip tip = findTipByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(tip.getIdFarm());
        validateTipReadPermission(tip, farm, principal, "visualizar esta dica técnica");

        List<TipResponseDTO> enriched = enrichTips(List.of(tip));
        return enriched.get(0);
    }

    /**
     * Atualiza parcialmente uma dica técnica existente (texto e/ou categorias).
     *
     * @param id        identificador único da dica
     * @param request   payload de atualização
     * @param principal dados do usuário logado
     * @return DTO com os dados atualizados
     */
    @Transactional
    public TipResponseDTO updateTip(Long id, TipUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (!ADM.equals(role) && !COMPANY_EMPLOYEE.equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para atualizar dicas técnicas"
            );
        }

        Tip tip = findTipByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(tip.getIdFarm());
        validateFarmAccessPermission(farm, principal, "alterar dicas técnicas desta fazenda");

        if (!request.hasUpdates()) {
            return enrichTips(List.of(tip)).get(0);
        }

        if (request.tip() != null && !request.tip().isBlank()) {
            tip.setTip(request.tip());
        }

        if (request.categoryIds() != null) {
            List<Category> categories = validateAndFetchCategories(request.categoryIds());
            tipCategoryRepository.deleteByIdTip(tip.getId());

            for (Category cat : categories) {
                tipCategoryRepository.save(TipCategory.builder()
                        .idTip(tip.getId())
                        .idCategory(cat.getId())
                        .build());
            }
        }

        Tip updated = tipRepository.save(tip);
        return enrichTips(List.of(updated)).get(0);
    }

    /**
     * Remove uma dica técnica e suas associações do sistema.
     *
     * @param id        identificador único da dica
     * @param principal dados do usuário logado
     */
    @Transactional
    public void deleteTip(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (!ADM.equals(role) && !COMPANY_EMPLOYEE.equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para remover dicas técnicas"
            );
        }

        Tip tip = findTipByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(tip.getIdFarm());
        validateFarmAccessPermission(farm, principal, "remover dica técnica desta fazenda");

        tipCategoryRepository.deleteByIdTip(tip.getId());
        farmTipRepository.deleteByIdTip(tip.getId());
        reviewRepository.deleteByIdTip(tip.getId());
        tipRepository.delete(tip);
    }

    private List<TipResponseDTO> getTipsForSingleFarm(Farm farm) {
        List<Tip> directTips = tipRepository.findByIdFarm(farm.getId());
        List<FarmTip> linkedFarmTips = farmTipRepository.findByIdFarm(farm.getId());
        List<Long> linkedTipIds = linkedFarmTips.stream().map(FarmTip::getIdTip).toList();
        List<Tip> junctionTips = linkedTipIds.isEmpty() ? List.of() : tipRepository.findAllById(linkedTipIds);

        Map<Long, Tip> combinedTips = new LinkedHashMap<>();
        directTips.forEach(t -> combinedTips.put(t.getId(), t));
        junctionTips.forEach(t -> combinedTips.putIfAbsent(t.getId(), t));

        return enrichTips(new ArrayList<>(combinedTips.values()));
    }

    private List<TipResponseDTO> enrichTips(List<Tip> tips) {
        if (tips.isEmpty()) {
            return List.of();
        }

        List<Long> tipIds = tips.stream().map(Tip::getId).toList();

        List<TipCategory> tipCategories = tipCategoryRepository.findByIdTipIn(tipIds);
        List<Long> categoryIds = tipCategories.stream().map(TipCategory::getIdCategory).distinct().toList();
        Map<Long, String> categoryMap = categoryIds.isEmpty() ? Map.of() :
                categoryRepository.findByIdIn(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, Category::getCategory, (c1, c2) -> c1));

        Map<Long, List<String>> categoriesByTipId = new HashMap<>();
        for (TipCategory tc : tipCategories) {
            String categoryName = categoryMap.get(tc.getIdCategory());
            if (categoryName != null) {
                categoriesByTipId.computeIfAbsent(tc.getIdTip(), k -> new ArrayList<>()).add(categoryName);
            }
        }

        List<Review> reviews = reviewRepository.findByIdTipIn(tipIds);
        Map<Long, List<Review>> reviewsByTipId = reviews.stream()
                .collect(Collectors.groupingBy(Review::getIdTip));

        return tips.stream().map(tip -> {
            List<String> tipCatNames = categoriesByTipId.getOrDefault(tip.getId(), List.of())
                    .stream().distinct().toList();
            List<Review> tipReviews = reviewsByTipId.getOrDefault(tip.getId(), List.of());
            long totalReviews = tipReviews.size();
            double avgRating = tipReviews.isEmpty() ? 0.0 :
                    BigDecimal.valueOf(tipReviews.stream().mapToInt(Review::getRating).average().orElse(0.0))
                            .setScale(1, RoundingMode.HALF_UP)
                            .doubleValue();

            return TipResponseDTO.fromEntity(tip, tipCatNames, totalReviews, avgRating);
        }).toList();
    }

    private List<Category> validateAndFetchCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }

        List<Category> categories = categoryRepository.findByIdIn(categoryIds);
        if (categories.size() != categoryIds.stream().distinct().count()) {
            Set<Long> foundIds = categories.stream().map(Category::getId).collect(Collectors.toSet());
            Long missingId = categoryIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Categoria não encontrada para o ID: " + missingId
            );
        }
        return categories;
    }

    private Long resolveFarmIdForCreation(Long idFarm, UserPrincipal principal) {
        if (idFarm != null) {
            return idFarm;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "O ID da fazenda é obrigatório para cadastrar uma dica técnica"
        );
    }

    private void validateTipReadPermission(Tip tip, Farm primaryFarm, UserPrincipal principal, String action) {
        ensureAuthenticated(principal);

        boolean isAuthorized = switch (principal.getRole()) {
            case ADM -> true;
            case COMPANY_EMPLOYEE -> {
                Long idEnterprise = getCompanyEmployeeOrThrow(principal.getId()).getIdEnterprise();
                boolean isPrimaryFarmOfEnterprise = Objects.equals(primaryFarm.getIdEnterprise(), idEnterprise);
                boolean isSharedWithEnterprise = farmTipRepository.findByIdTip(tip.getId()).stream()
                        .map(ft -> findFarmByIdOrThrow(ft.getIdFarm()))
                        .anyMatch(f -> Objects.equals(f.getIdEnterprise(), idEnterprise));
                yield isPrimaryFarmOfEnterprise || isSharedWithEnterprise;
            }
            case FARM_OWNER -> {
                FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
                yield Objects.equals(primaryFarm.getId(), owner.getIdFarm())
                        || (owner.getIdFarm() != null && farmTipRepository.existsByIdFarmAndIdTip(owner.getIdFarm(), tip.getId()));
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para acessar esta dica técnica"
            );
        };

        if (!isAuthorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para " + action);
        }
    }

    private void validateFarmAccessPermission(Farm farm, UserPrincipal principal, String action) {
        ensureAuthenticated(principal);

        boolean isAuthorized = switch (principal.getRole()) {
            case ADM -> true;
            case COMPANY_EMPLOYEE -> Objects.equals(
                    farm.getIdEnterprise(),
                    getCompanyEmployeeOrThrow(principal.getId()).getIdEnterprise()
            );
            case FARM_OWNER -> Objects.equals(
                    farm.getId(),
                    getFarmOwnerOrThrow(principal.getId()).getIdFarm()
            );
            default -> throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para acessar esta fazenda"
            );
        };

        if (!isAuthorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para " + action);
        }
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    private CompanyEmployee getCompanyEmployeeOrThrow(Long employeeId) {
        return companyEmployeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário logado não encontrado para o ID: " + employeeId
                ));
    }

    private FarmOwner getFarmOwnerOrThrow(Long ownerId) {
        return farmOwnerRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produtor rural logado não encontrado para o ID: " + ownerId
                ));
    }

    private Farm findFarmByIdOrThrow(Long farmId) {
        if (farmId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada para o ID: null");
        }
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fazenda não encontrada para o ID: " + farmId
                ));
    }

    private Tip findTipByIdOrThrow(Long tipId) {
        if (tipId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dica técnica não encontrada para o ID: null");
        }
        return tipRepository.findById(tipId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dica técnica não encontrada para o ID: " + tipId
                ));
    }
}
