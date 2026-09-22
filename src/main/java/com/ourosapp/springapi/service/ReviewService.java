package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.review.ReviewRequestDTO;
import com.ourosapp.springapi.dto.review.ReviewResponseDTO;
import com.ourosapp.springapi.dto.review.ReviewUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.Review;
import com.ourosapp.springapi.entity.Tip;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.ReviewRepository;
import com.ourosapp.springapi.repository.TipRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pelas regras de negócio e operações de persistência da entidade {@link Review} (Avaliação de Dica Técnica).
 * Implementa validações rigorosas de segurança, isolamento por tenant (multi-tenancy) e integridade de dados.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TipRepository tipRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;
    private final com.ourosapp.springapi.repository.FarmTipRepository farmTipRepository;

    /**
     * Cadastra uma nova avaliação/review para uma dica técnica específica.
     * Valida as permissões do usuário logado conforme a fazenda vinculada à dica.
     *
     * @param tipId     identificador único da dica técnica a ser avaliada
     * @param request   payload com o comentário e nota da avaliação
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados da avaliação cadastrada
     * @throws ResponseStatusException HTTP 400 se a nota ou comentário forem inválidos
     * @throws ResponseStatusException HTTP 401 se o usuário não estiver autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para avaliar a dica
     * @throws ResponseStatusException HTTP 404 se a dica técnica não for encontrada
     * @throws ResponseStatusException HTTP 409 se houver violação de integridade de dados
     */
    @Transactional
    public ReviewResponseDTO createReview(Long tipId, ReviewRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Tip tip = findTipByIdOrThrow(tipId);
        validateTipAccess(tip, principal, "avaliar");

        if (request.rating() != null && (request.rating() < 0 || request.rating() > 5)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A nota da avaliação deve estar entre 0 e 5"
            );
        }

        Review review = Review.builder()
                .comment(request.comment())
                .rating(request.rating())
                .idTip(tip.getId())
                .build();

        Review saved = reviewRepository.save(review);
        return ReviewResponseDTO.fromEntity(saved);
    }

    /**
     * Lista todas as avaliações registradas para uma dica técnica específica.
     *
     * @param tipId     identificador único da dica técnica
     * @param principal dados do usuário logado extraídos do token JWT
     * @return lista de DTOs com as avaliações da dica
     * @throws ResponseStatusException HTTP 401 se o usuário não estiver autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para consultar
     * @throws ResponseStatusException HTTP 404 se a dica técnica não for encontrada
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByTipId(Long tipId, UserPrincipal principal) {
        ensureAuthenticated(principal);

        Tip tip = findTipByIdOrThrow(tipId);
        validateTipAccess(tip, principal, "consultar avaliações de");

        return reviewRepository.findByIdTip(tip.getId())
                .stream()
                .map(ReviewResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Busca os detalhes de uma avaliação específica pelo seu ID.
     *
     * @param id        identificador único da avaliação
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os detalhes da avaliação
     * @throws ResponseStatusException HTTP 401 se o usuário não estiver autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão de acesso
     * @throws ResponseStatusException HTTP 404 se a avaliação não for encontrada
     */
    @Transactional(readOnly = true)
    public ReviewResponseDTO getReviewById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        Review review = findReviewByIdOrThrow(id);
        Tip tip = findTipByIdOrThrow(review.getIdTip());
        validateTipAccess(tip, principal, "consultar");

        return ReviewResponseDTO.fromEntity(review);
    }

    /**
     * Atualiza parcialmente uma avaliação existente (PATCH /reviews/{id}).
     *
     * @param id        identificador único da avaliação a ser atualizada
     * @param request   payload com os campos parciais
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com a avaliação atualizada
     * @throws ResponseStatusException HTTP 400 se a nova nota for inválida
     * @throws ResponseStatusException HTTP 401 se o usuário não estiver autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para alterar
     * @throws ResponseStatusException HTTP 404 se a avaliação não for encontrada
     */
    @Transactional
    public ReviewResponseDTO updateReview(Long id, ReviewUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Review review = findReviewByIdOrThrow(id);
        Tip tip = findTipByIdOrThrow(review.getIdTip());
        validateTipAccess(tip, principal, "alterar esta avaliação");

        if (!request.hasUpdates()) {
            return ReviewResponseDTO.fromEntity(review);
        }

        if (request.rating() != null && (request.rating() < 0 || request.rating() > 5)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A nota da avaliação deve estar entre 0 e 5"
            );
        }

        if (request.comment() != null && !request.comment().isBlank()) {
            review.setComment(request.comment());
        }
        if (request.rating() != null) {
            review.setRating(request.rating());
        }

        Review updated = reviewRepository.save(review);
        return ReviewResponseDTO.fromEntity(updated);
    }

    /**
     * Remove uma avaliação do sistema (DELETE /reviews/{id}).
     *
     * @param id        identificador único da avaliação a ser removida
     * @param principal dados do usuário logado extraídos do token JWT
     * @throws ResponseStatusException HTTP 401 se o usuário não estiver autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para excluir
     * @throws ResponseStatusException HTTP 404 se a avaliação não for encontrada
     * @throws ResponseStatusException HTTP 409 se houver conflito de integridade
     */
    @Transactional
    public void deleteReview(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        Review review = findReviewByIdOrThrow(id);
        Tip tip = findTipByIdOrThrow(review.getIdTip());
        validateTipAccess(tip, principal, "remover esta avaliação");

        reviewRepository.delete(review);
        reviewRepository.flush();
    }

    /**
     * Valida o isolamento por tenant (multi-tenancy) e permissões de acesso com base no vínculo da fazenda da dica.
     *
     * @param tip       dica técnica associada à avaliação
     * @param principal dados do usuário logado
     * @param action    descrição textual da ação
     */
    private void validateTipAccess(Tip tip, UserPrincipal principal, String action) {
        String role = principal.getRole();

        if (ADM.equals(role)) {
            return;
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            Farm primaryFarm = findFarmByIdOrThrow(tip.getIdFarm());
            boolean isPrimaryFarmOfEnterprise = Objects.equals(primaryFarm.getIdEnterprise(), employee.getIdEnterprise());
            boolean isSharedWithEnterprise = farmTipRepository.findByIdTip(tip.getId()).stream()
                    .map(ft -> findFarmByIdOrThrow(ft.getIdFarm()))
                    .anyMatch(f -> Objects.equals(f.getIdEnterprise(), employee.getIdEnterprise()));

            if (!isPrimaryFarmOfEnterprise && !isSharedWithEnterprise) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " da dica técnica vinculada a outra empresa"
                );
            }
            return;
        }

        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (!Objects.equals(tip.getIdFarm(), owner.getIdFarm()) && 
                (owner.getIdFarm() == null || !farmTipRepository.existsByIdFarmAndIdTip(owner.getIdFarm(), tip.getId()))) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " da dica técnica vinculada a outra fazenda"
                );
            }
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para " + action + " avaliações"
        );
    }

    /**
     * Garante que o usuário autenticado esteja presente no contexto de segurança.
     *
     * @param principal dados do usuário logado
     */
    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    /**
     * Busca o funcionário da integradora pelo ID ou lança HTTP 404 Not Found.
     *
     * @param id identificador do funcionário
     * @return funcionário encontrado
     */
    private CompanyEmployee getCompanyEmployeeOrThrow(Long id) {
        return companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário logado não encontrado para o ID: " + id
                ));
    }

    /**
     * Busca o produtor rural pelo ID ou lança HTTP 404 Not Found.
     *
     * @param id identificador do produtor
     * @return produtor rural encontrado
     */
    private FarmOwner getFarmOwnerOrThrow(Long id) {
        return farmOwnerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produtor rural logado não encontrado para o ID: " + id
                ));
    }

    /**
     * Busca a fazenda pelo ID ou lança HTTP 404 Not Found.
     *
     * @param id identificador da fazenda
     * @return fazenda encontrada
     */
    private Farm findFarmByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Fazenda não encontrada para o ID: null"
            );
        }
        return farmRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fazenda não encontrada para o ID: " + id
                ));
    }

    /**
     * Busca a dica técnica pelo ID ou lança HTTP 404 Not Found.
     *
     * @param id identificador da dica técnica
     * @return dica técnica encontrada
     */
    private Tip findTipByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Dica técnica não encontrada para o ID: null"
            );
        }
        return tipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dica técnica não encontrada para o ID: " + id
                ));
    }

    /**
     * Busca a avaliação pelo ID ou lança HTTP 404 Not Found.
     *
     * @param id identificador da avaliação
     * @return avaliação encontrada
     */
    private Review findReviewByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Avaliação não encontrada para o ID: null"
            );
        }
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Avaliação não encontrada para o ID: " + id
                ));
    }
}
